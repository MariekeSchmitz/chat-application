package de.mi.hsrm.chatclient;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

public class ChatService {

    private Client client; 
    private DatagramSocket udpSocket;
    private String udpHost;
    private int udpPort;

    public static final int DEFAULT_UDP_PORT = new Random().nextInt(15000, 20000);

    public ChatService (Client client) {
        this.client = client;
    }

    public void getReadyForChat() {

        if (udpSocket == null) {

            System.out.println("Getting ready for chat");

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
        
        while (!chatmessage.equals("!disconnect")) {
                
            byte[] receiveData = new byte[1024];

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
    
                // print message from chat 
                System.out.println(chatmessage);
                
                // receive new messages while chat is not disconnected 
                chatmessage = new String(receivePacket.getData()).substring(0, receivePacket.getLength());

            } catch (IOException e) {
                e.printStackTrace();
            }    

        }

        // when chat is disconnected, udp connection is closed and socket, host, port reset
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

        final int CHUNK_SIZE = 512;
        final int HEADER_SIZE = 3;
        final int IMAGE_CHUNK_SIZE = CHUNK_SIZE - HEADER_SIZE;

        BufferedImage img;
        try {
            img = ImageIO.read(new File(path));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();        
            ImageIO.write(img, "JPG", baos);
            baos.flush();

            byte[] totalImage = baos.toByteArray();
            byte[] chunk;
            byte counter = 0;
            byte numPackages = 0;
            byte imageIdentifier = 1;
            
            numPackages = (byte)(totalImage.length/CHUNK_SIZE);  
            
            // split image in image-chunks-Array
            List <List<Byte>> imageChunks = new ArrayList<>();

            for (int i = 0; i < numPackages; i++) {
                
                List <Byte> tempChunk = new ArrayList<>();

                for (int y = 0; y < IMAGE_CHUNK_SIZE; y++) {
                    tempChunk.add(y, totalImage[y + (i * IMAGE_CHUNK_SIZE)]); 
                }

                imageChunks.add(i, tempChunk);
            }

            // add additional package with smaller size (rest)
            if (totalImage.length % CHUNK_SIZE != 0) {

                int rest = totalImage.length % CHUNK_SIZE;
                numPackages++;

                List <Byte> tempChunk = new ArrayList<>();
                for (int y = 0; y < rest; y++) {
                    tempChunk.add(y, totalImage[y + ((numPackages-1) * IMAGE_CHUNK_SIZE)]); 
                }
            }

            // Gedankenbrücke:
            // 145 total, 50 size
            // == 2 packages (i = 0, i= 1)
            // drittes package: 45 mit i = 2 (0+2*50 = 100)

            // send imagechunks 
            for (counter = 0; counter < numPackages; counter++) {

                chunk = new byte[imageChunks.get(counter).size() + HEADER_SIZE];
                
                chunk[0] = imageIdentifier;
                chunk[1] = numPackages;
                chunk[2] = counter;

                for(int i = 0; i < imageChunks.get(counter).size(); i++) {
                    chunk[i + HEADER_SIZE] = imageChunks.get(counter).get(i);
                }

                InetAddress address = InetAddress.getByName(udpHost);
                DatagramPacket sendPacket = new DatagramPacket(chunk, chunk.length, address, udpPort);

                udpSocket.send(sendPacket);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        

        

        // byte[] sendData = line.getBytes();

        // try {

        //     InetAddress address = InetAddress.getByName(udpHost);
        //     DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, udpPort);

        //     udpSocket.send(sendPacket);

        // } catch (IOException e) {
        //     e.printStackTrace();
        // }


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
