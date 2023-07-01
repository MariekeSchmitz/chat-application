package de.mi.hsrm.chatclient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;

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
