package de.mi.hsrm.chatclient.ServerCommunication;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import de.mi.hsrm.chatclient.Client;

public class ServerRequestHandler {
    
    private Client client;

    private Socket tcp_socket;
    private BufferedWriter tcp_writer;
    private BufferedReader tcp_reader;

    public ServerRequestHandler(Client client) {
        this.client = client;
    }


    // setup TCP connection with clients host and port --> create socket, reader and writer
    public void enableTcpConnection() {

        String host = client.getTcpHost();
        int port = client.getTcpPort();

        try {
            tcp_socket = new Socket(host, port);
            tcp_reader = new BufferedReader(new InputStreamReader(tcp_socket.getInputStream()));
            tcp_writer = new BufferedWriter(new OutputStreamWriter(tcp_socket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    





}
