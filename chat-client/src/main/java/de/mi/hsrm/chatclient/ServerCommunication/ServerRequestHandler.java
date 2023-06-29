package de.mi.hsrm.chatclient.ServerCommunication;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
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

    // request to logout 
    public void sendLogoutRequest() throws IOException {
        tcpWriter.write("LOGOUT");
        tcpWriter.newLine();
        tcpWriter.flush();
    }

    // request to show users 
    public void sendShowUsersRequest() throws IOException {

        tcpWriter.write("REQUEST_ACTIVES");
        tcpWriter.newLine();
        tcpWriter.flush();

        String response = tcpReader.readLine();
        serverResponseHandler.handleTCPResponse(response);
    }

    // request to login 
    public void sendShowInvitationsRequest() throws IOException {

        tcpWriter.write("SHOW_INVITES");
        tcpWriter.newLine();
        tcpWriter.flush();

        String response = tcpReader.readLine();
        serverResponseHandler.handleTCPResponse(response);

    }


    // send request to invite another user by username
    public void sendInviteRequest(String username) throws IOException {

        String ownIPAddress = InetAddress.getLocalHost().getHostAddress();
        
        // send invite + own ip and port to enable udp connection
        tcpWriter.write("INVITE " + username + " " + ownIPAddress + " " + Client.DEFAULT_UDP_PORT);
        tcpWriter.newLine();
        tcpWriter.flush();

        String response = tcpReader.readLine();
        serverResponseHandler.handleTCPResponse(response);

    }


    // send request to deny a request a user has sent to me (chat invite)
    public void sendDenyRequest(String username) throws IOException {

        tcpWriter.write("DECLINE " + username);
        tcpWriter.newLine();
        tcpWriter.flush();

        String response = tcpReader.readLine();
        serverResponseHandler.handleTCPResponse(response);

    }

    // send request to accept a request a user has sent to me (chat invite)
    public void sendAcceptRequest(String username) throws IOException {

        tcpWriter.write("ACCEPT " + username);
        tcpWriter.newLine();
        tcpWriter.flush();

        String response = tcpReader.readLine();
        serverResponseHandler.handleTCPResponse(response);

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











}
