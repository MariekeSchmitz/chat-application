package de.mi.hsrm.chatclient;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.imageio.ImageIO;

public class ChatService {

    private Client client;
    private DatagramSocket udpSocket;
    private String udpHost;
    private int udpPort;
    private Map<Byte, boolean[]> ackedMessagesState;
    private byte messageCounter = 0;

    private Queue<byte[]>allChunksBuffer;
    // KEY: "0 1" VALUE: payload as byte-array
    // ACK: [2, 0, 1]

    private Map<String, byte[]> chunksInWindow;
    private byte[] previouslyAckedChunk = {0,-2,-1};

    public static final int DEFAULT_UDP_PORT = new Random().nextInt(15000, 20000);
    public static final int CHUNK_SIZE = 60000;
    public static final int HEADER_SIZE = 4;
    public static final int DATA_CHUNK_SIZE = CHUNK_SIZE - HEADER_SIZE;
    public static final byte TEXT_IDENTIFIER = 0;
    public static final byte IMAGE_IDENTIFIER = 1;
    public static final byte ACK_IDENTIFIER = 2;
    public static final int WINDOW_SIZE = 5;
    public static final int TIMEOUT = 500000;
    public static final boolean DEBUG_MODE_ON = true;

    public ChatService(Client client) {
        this.client = client;   
        this.allChunksBuffer = new LinkedList<>();
        this.chunksInWindow = new HashMap<>();
        // [messagenumber, messageSize, chunknumber]
    }

    public void getReadyForChat() {

        if (udpSocket == null) {

            System.out.println("Vorbereitungen für Chat werden getroffen...");
            ackedMessagesState = new HashMap<>();

            try {
                udpSocket = new DatagramSocket(DEFAULT_UDP_PORT);
                new Thread(() -> receiveChatMessage()).start();
            } catch (SocketException e) {
                e.printStackTrace();
            }

        }

    }

    // private void receiveAcks() {

    //     byte[] receiveData = new byte[CHUNK_SIZE];
    //     DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
    //     String receiveMessage;
       
    //         try {
    //             udpSocket.receive(receivePacket);
    //             if (receiveData[0] == ACK_IDENTIFIER) {
    //             ackedMessagesState.add(receiveData[1], true);
    //         }

    //         } catch (IOException e) {
    //             e.printStackTrace();
    //         }

    // }

    private void receiveChatMessage() {

        String chatmessage = "";
        int numChunks = 0;
        int numChunksReceived = 0;
        byte[] responseData;
        byte[][] receivedChunks = null;

        while (!chatmessage.equals("!disconnect")) {

            byte[] receiveData = new byte[CHUNK_SIZE];

            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            try {

                udpSocket.receive(receivePacket);

                if (udpHost == null) {
                    udpHost = receivePacket.getAddress().getHostAddress();
                }

                if (udpPort == 0) {
                    udpPort = receivePacket.getPort();
                }

                // show chat menu
                client.setActionMenuActive(false);
                client.setStatusMenuActive(false);

                if (!client.isChatMenuActive()) {
                    client.setChatMenuActive(true);
                    client.getUserIOService().showMenu();
                }

                // RECEIVE DATA 
                responseData = receivePacket.getData();
                
                // receive text or image
                if ((responseData[0] == TEXT_IDENTIFIER) || (responseData[0] == IMAGE_IDENTIFIER)) {
                    log("Du hast einen Text oder ein Bild erhalten");
                    
                    boolean receivedChunkMatchesExpectation;
                    byte previousMessageNumber = previouslyAckedChunk[0];
                    byte previousMessageSize = previouslyAckedChunk[1];
                    byte previousChunkNumber = previouslyAckedChunk[2];

                    byte newMessageNumber = responseData[1];
                    byte newMessageSize = responseData[2];
                    byte newChunkNumber = responseData[3];

                    // if (previousMessageSize == -1) {
                    //     receivedChunkMatchesExpectation = ((newChunkNumber == 0) && (newMessageNumber == 0));
                    // } 

                    log("previousMessageNumber " +previousMessageNumber);
                    log("previousMessageSize " +previousMessageSize);
                    log("previousChunkNumber " +previousChunkNumber);
                    log("newMessageNumber " +newMessageNumber);
                    log("newMessageSize " +newMessageSize);
                    log("newChunkNumber " +newChunkNumber);


                    // if the previously accepted chunk was the last one of its message, then compare incoming with chunk '0' and next messagenumber
                    if ((previousMessageSize - 1) == previousChunkNumber) {
                        receivedChunkMatchesExpectation = (0 == newChunkNumber) && ((previousMessageNumber + 1) == newMessageNumber);
                    
                    // else compare incoming chunk with next chunk in same message
                    } else {
                        receivedChunkMatchesExpectation = ((previousChunkNumber + 1) == newChunkNumber) && (previousMessageNumber == newMessageNumber);
                    }

                    // if chunk is accepted
                    if(receivedChunkMatchesExpectation) {

                        log("Das erhaltene Chunk passt zu dem vorher eingetroffenen Chunk.");

                        if (receivedChunks == null) {
                            log("Dir wird jetzt eine Nachricht gesendet.");
                            numChunks = responseData[2];
                            receivedChunks = new byte[numChunks][];
                        }
    
                        log("Du hast einen Chunk mit Nummer " + newChunkNumber + " erhalten");
                        receivedChunks[newChunkNumber] = Arrays.copyOfRange(responseData, HEADER_SIZE, responseData.length);
                        numChunksReceived++;

                        // send ack for received chunk
                        sendACK(newMessageNumber,newChunkNumber);
                        this.previouslyAckedChunk[0] = newMessageNumber;
                        this.previouslyAckedChunk[1] = newMessageSize;
                        this.previouslyAckedChunk[2] = newChunkNumber;

                        log("ACK mit Nummern " + newMessageNumber + ", " + newChunkNumber +  " wird gesendet");
                        
                        // when all chunks of a message are received, rebuild message
                        if (numChunksReceived == numChunks) {
    
                            log("Letztes Chunk für Message " + newMessageNumber + " erhalten");
    
                            // rebuild original byte-Array and put into image
                            byte[] totalData = new byte[numChunks * DATA_CHUNK_SIZE];
                            for (int i = 0; i < numChunks; i++) {
                                System.arraycopy(receivedChunks[i], 0, totalData, i * DATA_CHUNK_SIZE, DATA_CHUNK_SIZE);
                            }
    
                            if (responseData[0] == IMAGE_IDENTIFIER) {
    
                                // turn byte array into image
                                ByteArrayInputStream bis = new ByteArrayInputStream(totalData);
                                BufferedImage bufferedImage = ImageIO.read(bis);
                                ImageIO.write(bufferedImage, "jpg", new File("output.jpg"));
                                log("Bild wurde als Datei gesichert.");
    
                            } else {
    
                                // turn byte array into text message
                                chatmessage = new String(totalData);
                                log("Chatnachricht wurde zu '" + chatmessage + "' zusammengesetzt.");
                                if(!chatmessage.equals("!disconnect")) {
                                    System.out.println(chatmessage);
                                }
            
                            }
    
                            // reset imagedata
                            receivedChunks = null;
                            numChunksReceived = 0;
                            numChunks = 0;
                        }
                    
                    } else {
                        // resend previous ack
                        log("Das erhaltene Chunk passt nicht zu dem vorher eingetroffenen Chunk. Das alte ACK wird deshalb nochmal neu gesendet.");
                        sendACK(previousMessageNumber,previousChunkNumber);
                    }

                } else {
                    // receive ack
                    udpSocket.setSoTimeout(TIMEOUT); 
                    byte messageNumber = responseData[1];
                    byte chunkNumber = responseData[2];

                    // for (byte key : ackedMessagesState.keySet()) {
                    //     log("Message: " + key);
                    //     for (boolean b: ackedMessagesState.get(key)) {
                    //         log("value: " + b);
                    //     }
                    //     log("");
                    // }

                    log("ACK für message " + messageNumber + " und chunk " + chunkNumber + " erhalten.");

                    boolean[] acksReceivedForMessage = ackedMessagesState.get(messageNumber);
                    
                    // mark chunk as acked
                    acksReceivedForMessage[chunkNumber] = true;

                    // remove chunk from window 
                    chunksInWindow.remove(messageNumber + " " + chunkNumber);
                    sendNextChunkFromBuffer();
                    
                    List<String> keysToBeRemoved = new LinkedList<>();

                    // if a chunk with a lower message/chunk number has not been acked yet, remove it from window as well (meaning: ack was lost on the way)
                    chunksInWindow.forEach((key, value) -> {
                        
                        String[]splittedKey = key.split(" ");

                        byte messageNumberInKey = Byte.parseByte(splittedKey[0]);
                        byte chunkNumberInKey = Byte.parseByte(splittedKey[1]);

                        if (messageNumberInKey < messageNumber) {
                            keysToBeRemoved.add(key);

                        } else if (messageNumberInKey == messageNumber) {
                            if (chunkNumberInKey < chunkNumber) {
                                keysToBeRemoved.add(key);
                            }
                        }
                    });
                    
                    // for (String key : chunksInWindow.keySet()) {
                        
                    //     String[]splittedKey = key.split(" ");

                    //     byte messageNumberInKey = Byte.parseByte(splittedKey[0]);
                    //     byte chunkNumberInKey = Byte.parseByte(splittedKey[1]);

                    //     if (messageNumberInKey < messageNumber) {
                    //         keysToBeRemoved.add(key);

                    //     } else if (messageNumberInKey == messageNumber) {
                    //         if (chunkNumberInKey < chunkNumber) {
                    //             keysToBeRemoved.add(key);
                    //         }
                    //     }
                    // }

                    for (String key : keysToBeRemoved) {
                        chunksInWindow.remove(key);
                        sendNextChunkFromBuffer();
                    }
                    
                    // check if all chunks of a message have arrived
                    boolean allChunksAcked = true;
                    for (boolean chunkAcked : acksReceivedForMessage) {
                        if (!chunkAcked) {
                            log("Es sind noch nicht alle ACKs für Message " + messageNumber + " angekommen.");
                            allChunksAcked = false;
                            break;
                        }
                    }

                    if (allChunksAcked) {
                        ackedMessagesState.remove(messageNumber);
                        log("Deine gesamte letzte Nachricht ist bei deinem Chat-Partner angekommen.");
                    }
                }

            } catch (SocketException | SocketTimeoutException e) {
                resendWindow();
            } catch (IOException e) {
                e.printStackTrace();
            } 


        }

        // when chat is disconnected, udp connection is closed and socket, host, port
        // reset
        udpSocket.close();
        udpSocket = null;
        udpHost = null;
        udpPort = 0;

        // close chat view and show current menu
        client.setChatMenuActive(false);
        client.getUserIOService().showMenu();

    }

    private void resendWindow() {
        for (byte[] chunk : chunksInWindow.values()) {
            try {
                sendMessage(chunk);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendNextChunkFromBuffer() {

        // get next chunk from buffer
        if (allChunksBuffer.size() != 0) {
            byte[] nextChunkFromBuffer = allChunksBuffer.remove();
            byte messageNumber = nextChunkFromBuffer[1];
            byte chunkNumber = nextChunkFromBuffer[3];
    
            // put chunk in window and send it
            chunksInWindow.put(messageNumber + " " + chunkNumber, nextChunkFromBuffer);
            try {
                sendMessage(nextChunkFromBuffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    

    private void sendACK(byte messageNumber, byte chunkNumber) {

        byte[] sendData = {ACK_IDENTIFIER, messageNumber, chunkNumber};
        try {
            sendMessage(sendData);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    
    private void sendMessage(byte[]chunk) throws IOException {

        InetAddress address = InetAddress.getByName(udpHost);
        DatagramPacket sendPacket = new DatagramPacket(chunk, chunk.length, address, udpPort);
        udpSocket.send(sendPacket);

    }

    public void prepareSendMessageAndTimer(byte messageType, String data) {
        try {
            // set initial timeout
            if (messageCounter == 0) {
                udpSocket.setSoTimeout(TIMEOUT);
            } 
            prepareSendMessage(messageType, data);
        } catch (SocketException e) {
            resendWindow();
        }
    }

    private void prepareSendMessage(byte messageType, String data) {

        byte numChunks = 0;
        byte[] chunk;
        byte[] totalData;

        if (messageType == TEXT_IDENTIFIER) {
            totalData = data.getBytes();
        } else {
            try {
                BufferedImage img = ImageIO.read(new File(data));
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "JPG", baos);
                baos.flush();
                totalData = baos.toByteArray();
            } catch (IOException e) {
                return;
            }
        }

        numChunks = (byte) (totalData.length / DATA_CHUNK_SIZE);
        int rest = totalData.length % DATA_CHUNK_SIZE;
        if (rest != 0) {
            numChunks++;
        }

        log("num chunks: " + numChunks);

        ackedMessagesState.put(messageCounter, new boolean[numChunks]);
        // 0 [false,false]

        for (byte i = 0; i < numChunks; i++) {

            if (i == (numChunks - 1) && (rest != 0)) {

                // for last chunk size might be smaller
                chunk = new byte[HEADER_SIZE + rest];
                System.arraycopy(totalData, i * DATA_CHUNK_SIZE, chunk, HEADER_SIZE, rest);

            } else {
                chunk = new byte[CHUNK_SIZE];
                // copy chunk of totalImage into chunk-Array, takes (input-array, starting index
                // in input-array, output-array, starting index in output-array, number of
                // copied elements)
                System.arraycopy(totalData, i * DATA_CHUNK_SIZE, chunk, HEADER_SIZE, DATA_CHUNK_SIZE);
            }

            // add meta data that enable receiving client to put chunks back together
            chunk[0] = messageType;
            chunk[1] = messageCounter;
            chunk[2] = numChunks;
            chunk[3] = i;

            log("Header: – messagetype: " + messageType + " – messageCounter: " + messageCounter + " – numChunks: " + numChunks + " – chunknumber: " + i);

            try {
                // check if chunk can already be sent
                if (allChunksBuffer.size() < WINDOW_SIZE) {
                    sendMessage(chunk);
 
                    // add chunk to array that shows if ack per chunk arrived
                    ackedMessagesState.get(messageCounter)[i] = false;

                    // add chunk to window
                    chunksInWindow.put(messageCounter + " " + i, chunk);
                    log("Chunk wurde gesendet.");

                } else {
                    // add chunk to entire buffer
                    allChunksBuffer.add(chunk);
                    log("Chunk konnte grade noch nicht gesendet werden, aber ist im Buffer gespeichert.");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
          
        }

        messageCounter++;
        
        // try {
        //     img = ImageIO.read(new File(data));
        //     ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //     ImageIO.write(img, "JPG", baos);
        //     baos.flush();

        //     byte[] totalImage = baos.toByteArray();
        //     byte numPackages = 0;
        //     byte[] chunk;

        //     numPackages = (byte) (totalImage.length / CHUNK_SIZE);
        //     if (totalImage.length % CHUNK_SIZE != 0) {
        //         numPackages++;
        //     }

        //     for (byte i = 0; i < numPackages; i++) {

        //         if (i == (numPackages - 1) && totalImage.length % CHUNK_SIZE != 0) {

        //             // for last chunk size might be smaller
        //             int rest = totalImage.length % CHUNK_SIZE;
        //             chunk = new byte[HEADER_SIZE + rest];
        //             System.arraycopy(totalImage, i * IMAGE_CHUNK_SIZE, chunk, HEADER_SIZE, rest);

        //         } else {
        //             chunk = new byte[CHUNK_SIZE];
        //             // copy chunk of totalImage into chunk-Array, takes (input-array, starting index
        //             // in input-array, output-array, starting index in output-array, number of
        //             // copied elements)
        //             System.arraycopy(totalImage, i * IMAGE_CHUNK_SIZE, chunk, HEADER_SIZE, IMAGE_CHUNK_SIZE);
        //         }

        //         // add meta data that enable receiving client to put chunks back together
        //         chunk[0] = IMAGE_IDENTIFIER;
        //         chunk[1] = numPackages;
        //         chunk[2] = i;

        //         InetAddress address = InetAddress.getByName(udpHost);
        //         DatagramPacket sendPacket = new DatagramPacket(chunk, chunk.length, address, udpPort);

        //         udpSocket.send(sendPacket);



        //     }

        // } catch (IOException e) {
        //     e.printStackTrace();
        // }

    }

    private void log(String message){
        if(DEBUG_MODE_ON) {
            System.out.println("LOG –   " + message);
        }
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public DatagramSocket getUdpSocket() {
        return udpSocket;
    }

    public void setUdpSocket(DatagramSocket udpSocket) {
        this.udpSocket = udpSocket;
    }

    public String getUdpHost() {
        return udpHost;
    }

    public void setUdpHost(String udpHost) {
        this.udpHost = udpHost;
    }

    public int getUdpPort() {
        return udpPort;
    }

    public void setUdpPort(int udpPort) {
        this.udpPort = udpPort;
    }

    public static int getDefaultUdpPort() {
        return DEFAULT_UDP_PORT;
    }

}
