package de.mi.hsrm.chatserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ChatSession {

    private Map<String, String[]> invitations = new HashMap<>();
    private boolean isAvailable = true;

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    private String user;


    public ChatSession(Socket socket, BufferedReader reader, BufferedWriter writer) {
        this.socket = socket;
        this.reader = reader;
        this.writer = writer;
    }


    public void addInvitation(String username, String host, String port) {
        String[] data = { host, port };
        this.invitations.put(username, data);
    }


    public void removeInvitation(String username) {
        this.invitations.remove(username);
    }


    // public void sendSuccessMessage(String message) throws IOException {
    //     sendMessage("SUCCESS:" + message);
    // }


    public void sendErrorMessage(String message) throws IOException {
        sendMessage("ERROR:" + message);
    }


    public void sendMessage(String message) throws IOException {
        writer.write(message);
        writer.newLine();
        writer.flush();
    }


    public Socket getSocket() {
        return socket;
    }


    public BufferedReader getReader() {
        return reader;
    }


    public BufferedWriter getWriter() {
        return writer;
    }


    public String getUser() {
        return user;
    }


    public Map<String, String[]> getInvitations() {
        return invitations;
    }


    public boolean isAvailable() {
        return isAvailable;
    }


    public void setSocket(Socket socket) {
        this.socket = socket;
    }


    public void setReader(BufferedReader reader) {
        this.reader = reader;
    }


    public void setWriter(BufferedWriter writer) {
        this.writer = writer;
    }


    public void setUser(String user) {
        this.user = user;
    }


    public void setIsAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }
    
}
