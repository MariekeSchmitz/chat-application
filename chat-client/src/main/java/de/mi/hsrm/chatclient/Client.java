package de.mi.hsrm.chatclient;

import java.util.Random;

public class Client {

    public static final int DEFAULT_UDP_PORT = new Random().nextInt(15000, 20000);
    public static final int DEFAULT_TCP_PORT = 12345;
    public static final String DEFAULT_TCP_HOST = "localhost";
    
    private String tcpHost;
    private int tcpPort;

    private boolean isLoggedIn = false;
    private boolean isInStatusMenu = false;
    private boolean isInActionMenu = false;
    private boolean isInChatMenu = false;

    public Client(String host, int port) {
        tcpHost = host;
        tcpPort = port;
    }

    public String getTcpHost() {
        return tcpHost;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public boolean isInStatusMenu() {
        return isInStatusMenu;
    }

    public boolean isInActionMenu() {
        return isInActionMenu;
    }

    public boolean isInChatMenu() {
        return isInChatMenu;
    }

    public void setLoggedIn(boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
    }

    public void setInStatusMenu(boolean isInStatusMenu) {
        this.isInStatusMenu = isInStatusMenu;
    }

    public void setInActionMenu(boolean isInActionMenu) {
        this.isInActionMenu = isInActionMenu;
    }

    public void setInChatMenu(boolean isInChatMenu) {
        this.isInChatMenu = isInChatMenu;
    }

    

    

    
    
  

}