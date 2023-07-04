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
import java.util.Arrays;
import java.util.Random;

import javax.imageio.ImageIO;

public class ChatService {

    private Client client;
    private DatagramSocket udpSocket;
    private String udpHost;
    private int udpPort;

    public static final int DEFAULT_UDP_PORT = new Random().nextInt(15000, 20000);
    public static final int CHUNK_SIZE = 60000;
    public static final int HEADER_SIZE = 3;
    public static final int IMAGE_CHUNK_SIZE = CHUNK_SIZE - HEADER_SIZE;
    public static final byte IMAGE_IDENTIFIER = 1;

    public ChatService(Client client) {
        this.client = client;
    }

    public void getReadyForChat() {

        if (udpSocket == null) {

            System.out.println("Vorbereitungen für Chat werden getroffen...");

            try {

                udpSocket = new DatagramSocket(DEFAULT_UDP_PORT);
                new Thread(() -> receiveChatMessage()).start();

            } catch (SocketException e) {
                e.printStackTrace();
            }

        }

    }

    private void receiveChatMessage() {

        String chatmessage = "";
        int numChunks = 0;
        int numChunksReceived = 0;
        boolean imageIncoming = false;
        byte[] responseData;
        byte[][] imageChunks = null;

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

                if (!imageIncoming) {
                    // print previous message from chat
                    System.out.println(chatmessage);
                }

                // handle incoming image data
                if (responseData[0] == 1) {

                    System.out.println("Bilddaten erhalten");

                    if (imageChunks == null) {
                        numChunks = responseData[1];
                        imageChunks = new byte[numChunks][];
                        imageIncoming = true;
                    }

                    int chunkNumber = responseData[2];
                    imageChunks[chunkNumber] = Arrays.copyOfRange(responseData, HEADER_SIZE, responseData.length);
                    numChunksReceived++;

                    if (numChunksReceived == numChunks) {

                        System.out.println("Letzte Bilddaten erhalten");

                        // rebuild original byte-Array and put into image
                        byte[] imageTotal = new byte[numChunks * IMAGE_CHUNK_SIZE];
                        for (int i = 0; i < numChunks; i++) {
                            System.arraycopy(imageChunks[i], 0, imageTotal, i * IMAGE_CHUNK_SIZE, IMAGE_CHUNK_SIZE);
                        }

                        // to do: trim
                        ByteArrayInputStream bis = new ByteArrayInputStream(imageTotal);
                        BufferedImage bufferedImage = ImageIO.read(bis);
                        ImageIO.write(bufferedImage, "jpg", new File("output.jpg"));
                        System.out.println("Bild wurde als Datei gesichert.");

                        // reset imagedata
                        imageChunks = null;
                        imageIncoming = false;
                        numChunksReceived = 0;
                        numChunks = 0;
                    }

                } else {
                    // receive new messages while chat is not disconnected
                    chatmessage = new String(receivePacket.getData()).substring(0, receivePacket.getLength());

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

    public void sendChatMessage(String line) {

        byte[] sendData = line.getBytes();

        try {

            InetAddress address = InetAddress.getByName(udpHost);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, udpPort);

            udpSocket.send(sendPacket);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendImage(String path) {

        BufferedImage img;
        try {
            img = ImageIO.read(new File(path));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "JPG", baos);
            baos.flush();

            byte[] totalImage = baos.toByteArray();
            byte numPackages = 0;
            byte[] chunk;

            numPackages = (byte) (totalImage.length / CHUNK_SIZE);
            if (totalImage.length % CHUNK_SIZE != 0) {
                numPackages++;
            }

            for (byte i = 0; i < numPackages; i++) {

                if (i == (numPackages - 1) && totalImage.length % CHUNK_SIZE != 0) {

                    // for last chunk size might be smaller
                    int rest = totalImage.length % CHUNK_SIZE;
                    chunk = new byte[HEADER_SIZE + rest];
                    System.arraycopy(totalImage, i * IMAGE_CHUNK_SIZE, chunk, HEADER_SIZE, rest);

                } else {
                    chunk = new byte[CHUNK_SIZE];
                    // copy chunk of totalImage into chunk-Array, takes (input-array, starting index
                    // in input-array, output-array, starting index in output-array, number of
                    // copied elements)
                    System.arraycopy(totalImage, i * IMAGE_CHUNK_SIZE, chunk, HEADER_SIZE, IMAGE_CHUNK_SIZE);
                }

                // add meta data that enable receiving client to put chunks back together
                chunk[0] = IMAGE_IDENTIFIER;
                chunk[1] = numPackages;
                chunk[2] = i;

                InetAddress address = InetAddress.getByName(udpHost);
                DatagramPacket sendPacket = new DatagramPacket(chunk, chunk.length, address, udpPort);

                udpSocket.send(sendPacket);

            }

        } catch (IOException e) {
            e.printStackTrace();
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
