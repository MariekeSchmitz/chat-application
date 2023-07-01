package de.mi.hsrm.chatclient;
import java.io.BufferedReader;
import java.io.IOException;

import de.mi.hsrm.chatclient.ServerService.ServerRequestService;

public class Client {
    public static final int DEFAULT_TCP_PORT = 12345;
    public static final String DEFAULT_TCP_HOST = "localhost";
    
    private String tcpHost;
    private int tcpPort;

    private boolean isLoggedIn = false;

    private boolean isStatusMenuActive = false;
    private boolean isActionMenuActive = false;
    private boolean isChatMenuActive = false;

    private ServerRequestService serverRequestService;
    private ChatService chatService;
    private UserIOService userIOService;

    private AccessControlWrapper access;


    public Client(String host, int port) {
        tcpHost = host;
        tcpPort = port;
        access = new AccessControlWrapper(this);

        serverRequestService = new ServerRequestService(this);
        chatService = new ChatService(this);
        userIOService = new UserIOService(this);
    }

    public void start() {
        serverRequestService.enableTcpConnection();
        userIOService.showMenu();
        userIOService.startUserInput();
    }
    

    public void stop() {
        serverRequestService.shutdownTcpConnection();
    }


    public void performRegistration(BufferedReader reader) throws IOException {

        System.out.println("\nGib einen Nutzernamen ein.");
        String user = reader.readLine();
        
        System.out.println("\nGib ein Passwort ein.");
        String pass = reader.readLine();

        serverRequestService.sendRegisterRequest(user, pass);

    }


    public void performLogin(BufferedReader reader) throws IOException {

        System.out.println("\nGib deinen Nutzernamen ein.");

        String user = reader.readLine();

        System.out.println("\nGib dein Passwort ein.");
        String pass = reader.readLine();

        serverRequestService.sendLoginRequest(user, pass);

    }


    public void performInvite(BufferedReader reader) throws IOException {

        System.out.println("\nWelchen User möchtest du einladen?");
        String user = reader.readLine();

        serverRequestService.sendInviteRequest(user);

    }


    public void performAccept(BufferedReader reader) throws IOException {

        System.out.println("\nVon welchem Nutzer möchtest du die Einladung annehmen?");
        String user = reader.readLine();

        serverRequestService.sendAcceptRequest(user);
    }


    public void performDeny(BufferedReader reader) throws IOException {
        System.out.println("\nVon welchem Nutzer möchtest du die Einladung ablehnen?");

        String user = reader.readLine();

        serverRequestService.sendDenyRequest(user);
    }

    
    public void performLogout() throws IOException {
        serverRequestService.sendLogoutRequest();
        isLoggedIn = false;
        isActionMenuActive = false;
    }


    public void performShowUsers() throws IOException {
        serverRequestService.sendShowUsersRequest();
    }


    public void performShowInvitations() throws IOException {
        serverRequestService.sendShowInvitationsRequest();
    }


    public static int getDefaultTcpPort() {
        return DEFAULT_TCP_PORT;
    }

    public static String getDefaultTcpHost() {
        return DEFAULT_TCP_HOST;
    }

    public String getTcpHost() {
        return tcpHost;
    }

    public void setTcpHost(String tcpHost) {
        this.tcpHost = tcpHost;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public void setLoggedIn(boolean isLoggedIn) {
        this.isLoggedIn = isLoggedIn;
    }

    public boolean isStatusMenuActive() {
        return isStatusMenuActive;
    }

    public void setStatusMenuActive(boolean isInStatusMenu) {
        this.isStatusMenuActive = isInStatusMenu;
    }

    public boolean isActionMenuActive() {
        return isActionMenuActive;
    }

    public void setActionMenuActive(boolean isInActionMenu) {
        this.isActionMenuActive = isInActionMenu;
    }

    public boolean isChatMenuActive() {
        return isChatMenuActive;
    }

    public void setChatMenuActive(boolean isInChatMenu) {
        this.isChatMenuActive = isInChatMenu;
    }

    public ServerRequestService getServerRequestService() {
        return serverRequestService;
    }

    public void setServerRequestService(ServerRequestService serverRequestHandler) {
        this.serverRequestService = serverRequestHandler;
    }


    public AccessControlWrapper getAccess() {
        return access;
    }

    public void setAccess(AccessControlWrapper access) {
        this.access = access;
    }

    public ChatService getChatService() {
        return chatService;
    }


    public UserIOService getUserIOService() {
        return userIOService;
    }


}