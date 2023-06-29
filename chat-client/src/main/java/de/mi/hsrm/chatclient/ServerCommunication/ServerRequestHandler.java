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
    private ServerResponseHandler serverResponseHandler;

    private Socket tcpSocket;
    private BufferedWriter tcpWriter;
    private BufferedReader tcpReader;

    public ServerRequestHandler(Client client) {
        this.client = client;
        this.serverResponseHandler = new ServerResponseHandler(client);
    }

    // setup TCP connection with clients host and port --> create socket, reader and writer
    public void enableTcpConnection() {

        String host = client.getTcpHost();
        int port = client.getTcpPort();

        try {
            tcpSocket = new Socket(host, port);
            tcpReader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
            tcpWriter = new BufferedWriter(new OutputStreamWriter(tcpSocket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // close TCP connection (potentially including logout) and send stop session-info to server
    public void shutdownTcpConnection() {

        try {

            if (client.isLoggedIn()) {
                tcpWriter.write("LOGOUT");
                tcpWriter.newLine();
                tcpWriter.flush();
            }

            tcpWriter.write("END_SESSION");
            tcpWriter.newLine();
            tcpWriter.flush();
            
            if (tcpSocket != null) {
                tcpSocket.close();
            }
            
            if (tcpWriter != null) {
                tcpWriter.close();
            }
            
            if (tcpReader != null) {
                tcpReader.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // request to register on server with username and password
    public void sendRegisterRequest(String username, String password) throws IOException {

        tcpWriter.write("REGISTER " + username + " " + password);
        tcpWriter.newLine();
        tcpWriter.flush();

        String response = tcpReader.readLine();

        // send response to responseHandler
        serverResponseHandler.handleTCPResponse(response);
    }

    // request to login 
    public void sendLoginRequest(String username, String password) throws IOException {

        tcpWriter.write("LOGIN " + username + " " + password);
        tcpWriter.newLine();
        tcpWriter.flush();

        String response = tcpReader.readLine();

        // send response to responseHandler
        serverResponseHandler.handleTCPResponse(response);
    }











}
