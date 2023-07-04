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
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

public class ChatService {

    private Client client;
    private DatagramSocket udpSocket;
    private String udpHost;
    private int udpPort;
    private Map<Byte, boolean[]> ackedMessagesState;
    private byte messageCounter = 0;
    private Thread ackThread;

    public static final int DEFAULT_UDP_PORT = new Random().nextInt(15000, 20000);
    public static final int CHUNK_SIZE = 60000;
    public static final int HEADER_SIZE = 4;
    public static final int DATA_CHUNK_SIZE = CHUNK_SIZE - HEADER_SIZE;
    public static final byte TEXT_IDENTIFIER = 0;
    public static final byte IMAGE_IDENTIFIER = 1;
    public static final byte ACK_IDENTIFIER = 2;

    public static final boolean DEBUG_MODE_ON = true;

    public ChatService(Client client) {
        this.client = client;   
    }

    public void getReadyForChat() {

        if (udpSocket == null) {

            System.out.println("Vorbereitungen für Chat werden getroffen...");
            ackedMessagesState = new HashMap<>();

            try {

                udpSocket = new DatagramSocket(DEFAULT_UDP_PORT);
                new Thread(() -> receiveChatMessage()).start();
                // ackThread = new Thread(() -> {
                //     while(client.isChatMenuActive()) {
                //         receiveAcks();
                //     }
                // });    
                // ackThread.start();

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
        byte[][] receivedImageChunks = null;

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

                responseData = receivePacket.getData();


                // RECEIVE DATA 
                // receive text or image
                if ((responseData[0] == TEXT_IDENTIFIER) || (responseData[0] == IMAGE_IDENTIFIER)) {
                    
                    if (receivedImageChunks == null) {
                        log("Dir wird jetzt eine Nachricht gesendet.");
                        numChunks = responseData[2];
                        receivedImageChunks = new byte[numChunks][];
                    }

                    byte messageNumber = responseData[1];
                    byte chunkNumber = responseData[3];
                    log("Du hast einen Chunk mit Nummer " + chunkNumber + " erhalten");

                    receivedImageChunks[chunkNumber] = Arrays.copyOfRange(responseData, HEADER_SIZE, responseData.length);
                    numChunksReceived++;

                    // send ack for reveived chunk
                    sendACK(messageNumber,chunkNumber);
                    log("ACK mit Nummern " + messageNumber + ", " + chunkNumber +  " wird gesendet");

                    // when all chunks of a message are received, rebuild message
                    if (numChunksReceived == numChunks) {

                        log("Letztes Chunk für Message " + messageNumber + " erhalten");

                        // rebuild original byte-Array and put into image
                        byte[] imageTotal = new byte[numChunks * DATA_CHUNK_SIZE];
                        for (int i = 0; i < numChunks; i++) {
                            System.arraycopy(receivedImageChunks[i], 0, imageTotal, i * DATA_CHUNK_SIZE, DATA_CHUNK_SIZE);
                        }

                        if (responseData[0] == IMAGE_IDENTIFIER) {

                            // turn byte array into image
                            ByteArrayInputStream bis = new ByteArrayInputStream(imageTotal);
                            BufferedImage bufferedImage = ImageIO.read(bis);
                            ImageIO.write(bufferedImage, "jpg", new File("output.jpg"));
                            log("Bild wurde als Datei gesichert.");

                        } else {

                            // turn byte array into text message
                            chatmessage = new String(imageTotal);
                            log("Chatnachricht wurde zu '" + chatmessage + "' zusammengesetzt.");
                            if(!chatmessage.equals("!disconnect")) {
                                System.out.println(chatmessage);
                            }
        
                        }

                        // reset imagedata
                        receivedImageChunks = null;
                        numChunksReceived = 0;
                        numChunks = 0;
                    }
                    
                } else {
                    // receive ack
                    byte messageNumber = responseData[1];
                    byte chunkNumber = responseData[2];

                    for (byte key : ackedMessagesState.keySet()) {
                        log("Message: " + key);
                        for (boolean b: ackedMessagesState.get(key)) {
                            log("value: " + b);
                        }
                        log("");
                    }

                    log("ACK für message " + messageNumber + " und chunk " + chunkNumber + " erhalten.");

                    boolean[] acksReceivedForMessage = ackedMessagesState.get(messageNumber);
                    acksReceivedForMessage[chunkNumber] = true;

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


    private void sendACK(byte messageNumber, byte chunkNumber) {

        byte[] sendData = {ACK_IDENTIFIER, messageNumber, chunkNumber};
        InetAddress address;
        try {
            address = InetAddress.getByName(udpHost);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, udpPort);

            udpSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // public void sendChatMessage(String line) {
        
    //     // PROTOCOLL: TEXT-IDENTIFIER PACKAGE-INDEX DATA
    //     byte[] sendData = new byte[line.getBytes().length + 2];
    //     sendData[0] = TEXT_IDENTIFIER;
    //     sendData[1] = messageCounter;
    //     System.arraycopy(line.getBytes(), 0, sendData, 2, line.getBytes().length);

    //     ackedMessagesState.add(messageCounter, false);
    //     messageCounter++;

    //     try {

    //         InetAddress address = InetAddress.getByName(udpHost);
    //         DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, udpPort);

    //         udpSocket.send(sendPacket);

    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }

    // }

    public void sendMessage(byte messageType, String data) {

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
                InetAddress address = InetAddress.getByName(udpHost);
                DatagramPacket sendPacket = new DatagramPacket(chunk, chunk.length, address, udpPort);
                udpSocket.send(sendPacket);
                ackedMessagesState.get(messageCounter)[i] = false;
                log("Chunk wurde gesendet.");
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
