package de.mi.hsrm.chatserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    public static final int DEFAULT_PORT = 12345;
    public static final String USERDATA_FILE = "chat-server/src/main/resources/registeredUsers.txt";

    private Map<String, ChatSession> activeUsers = new ConcurrentHashMap<>();
    private Map<String, String> registeredUsers = new ConcurrentHashMap<>();

    private boolean serverRunning = true;

    private int port;

    public Server() {
        this(DEFAULT_PORT);
    }

    public Server(int port) {
        this.port = port;
        importUserdata();
    }

    public void start() {

        try (final ServerSocket server = new ServerSocket(port)) {

            while (serverRunning) {

                // listen to and accept incoming requests for connection
                Socket socket = server.accept();
                System.out.printf("Client connected %s%n", socket.getInetAddress());
                new Thread(() -> handleIncomingRequests(socket)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void stop() {
        serverRunning = false;
        exportUserdata();
    }

    private void handleIncomingRequests(Socket socket) {

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            // create session per incoming request (user)
            ChatSession chatsession = new ChatSession(socket, reader, writer);

            String line;

            // keep listening to the socket until session is ended
            do {
                line = reader.readLine();
                interpret(chatsession, line);
            } while (!line.equals("END_SESSION"));

            // When session is ended remove user from activeUser map
            String user = chatsession.getUser();
            if (user != null) {
                activeUsers.remove(user);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void interpret(ChatSession session, String line) throws IOException {

        String[] requestElements = line.split(":");
        String command = requestElements[0];

        String[] data = null;

        if (requestElements.length >= 2) {
            data = requestElements[1].split(" ");
        }

        switch (command.toUpperCase()) {

            case "REGISTER":
                handleRegistration(session, data);
                break;

            case "LOGIN":
                handleLogin(session, data);
                break;

            case "LOGOUT":
                handleLogout(session);
                break;

            case "REQUEST_ACTIVES":
                handleShowUsers(session);
                break;

            case "INVITE":
                handleInvite(session, data);
                break;

            case "SHOW_INVITES":
                handleShowInvites(session);
                break;

            case "ACCEPT":
                handleAcceptInvite(session, data);
                break;

            case "DECLINE":
                handleDenyInvite(session, data);
                break;

            default:
                session.sendErrorMessage("\nCommand nicht gefunden.");
                break;
        }

    }

    private void handleRegistration(ChatSession session, String[] data) throws IOException {

        if (data == null) {
            session.sendErrorMessage("Es wurden keine Nutzerdaten angegeben");
        }

        else if (data.length != 2) {
            session.sendErrorMessage("Username und Passwort werden benötigt.");
        }

        else {

            String user = data[0];
            String pass = data[1];

            if (registeredUsers.get(user) != null) {
                session.sendErrorMessage("User " + user + " ist schon registriert.");
            }

            else {
                registeredUsers.put(user, pass);
                session.sendMessage("REGISTER:" + user);
            }

        }

    }

    private void handleLogin(ChatSession session, String[] data) throws IOException {

        if (session.getUser() != null) {
            session.sendErrorMessage("Schon eingeloggt.");
        }

        else if (data == null) {
            session.sendErrorMessage("Keine Nutzerdaten angegeben.");
        }

        else if (data.length != 2) {
            session.sendErrorMessage("Bitte Nutzerdaten und Passwort angeben.");
        }

        else {

            String user = data[0];
            String pass = data[1];

            if (registeredUsers.get(user) == null) {
                session.sendErrorMessage(user + " ist noch nicht registriert.");
            }

            else if (!registeredUsers.get(user).equals(pass)) {
                session.sendErrorMessage("Falsches Passwort für " + user + ".");
            }

            else if (activeUsers.get(user) != null) {
                session.sendErrorMessage("User " + user + "ist schon eingeloggt.");
            }

            else {
                session.setUser(user);
                activeUsers.put(user, session);
                session.sendMessage("LOGIN:" + user);
            }
        }
    }

    private void handleLogout(ChatSession session) throws IOException {

        String user = session.getUser();

        if (user == null) {
            session.sendErrorMessage("Nicht eingeloggt.");
        }

        else {
            activeUsers.remove(user);
            session.setUser(null);
        }

    }

    private void handleShowUsers(ChatSession session) throws IOException {

        if (session.getUser() == null) {
            session.sendErrorMessage("Nicht eingeloggt.");
        }

        else {
            String users = String.join(";", activeUsers.keySet());
            session.sendMessage("REQUEST_ACTIVES:" + users);
        }
    }

    private void handleInvite(ChatSession session, String[] payload) throws IOException {

        String currentUser = session.getUser();

        if (currentUser == null) {
            session.sendErrorMessage("Nicht eingeloggt.");
        }

        else if (!session.isAvailable()) {
            session.sendErrorMessage("Du hast schon einen aktiven Chat.");
        }

        else if (payload == null) {
            session.sendErrorMessage("Es wurden keine Nutzerdaten angegeben.");
        }

        else if (payload.length > 3) {
            session.sendErrorMessage("Es braucht User, den du einladen willst und host und portnummer");
        }

        else {

            String user = payload[0];

            if (user.equals(currentUser)) {
                session.sendErrorMessage("Du kannst dich nicht selbst einladen.");
            }

            else if (!registeredUsers.containsKey(user)) {
                session.sendErrorMessage("User " + user + " existiert nicht.");
            }

            else if (!activeUsers.containsKey(user)) {
                session.sendErrorMessage("User " + user + " ist nicht verfügbar.");
            }

            else {
                ChatSession otherUserSession = activeUsers.get(user);

                if (!otherUserSession.isAvailable()) {
                    session.sendErrorMessage("User " + user + " ist nicht verfügbar.");
                }

                else {
                    String host = payload[1];
                    String port = payload[2];
                    otherUserSession.addInvitation(currentUser, host, port);
                    session.sendMessage("INVITE_SENT:" + user);
                }

            }
        }

    }

    private void handleShowInvites(ChatSession session) throws IOException {

        if (session.getUser() == null) {
            session.sendErrorMessage("Du bist nicht eingeloggt.");
        }

        else {
            String invites = String.join(";", session.getInvitations().keySet());
            session.sendMessage("SHOW_INVITES:" + invites);
        }
    }

    private void handleAcceptInvite(ChatSession session, String[] payload) throws IOException {

        String currentUser = session.getUser();

        if (currentUser == null) {
            session.sendErrorMessage("Nicht eingeloggt.");
        }

        else if (!session.isAvailable()) {
            session.sendErrorMessage("Du hast schon einen aktiven Chat.");
        }

        else if (payload == null) {
            session.sendErrorMessage("Es wurden keine Nutzerdaten angegeben.");
        }

        else if (payload.length > 1) {
            session.sendErrorMessage("Du kannst nur einen User einladen.");
        }

        else {

            String user = payload[0];

            if (!session.getInvitations().containsKey(user)) {
                session.sendErrorMessage("User " + user + " hat dich nicht eingeladen.");
            }

            else if (!activeUsers.containsKey(user)) {
                session.sendErrorMessage("User " + user + " ist nicht mehr aktiv.");
            }

            else {

                ChatSession otherUserSession = activeUsers.get(user);

                if (!otherUserSession.isAvailable()) {
                    session.sendErrorMessage("User " + user + " chattet mit jemand anderem.");
                }

                else {
                    otherUserSession.setIsAvailable(false);
                    session.setIsAvailable(false);

                    String data = String.join(";", session.getInvitations().get(user));

                    session.sendMessage("INVITE_ACCEPT:" + data);
                }

            }
        }
    }

    private void handleDenyInvite(ChatSession session, String[] payload) throws IOException {

        String currentUser = session.getUser();

        if (currentUser == null) {
            session.sendErrorMessage("Nicht eingeloggt.");
        }

        else if (payload == null) {
            session.sendErrorMessage("Keiner Nutzerdaten vorhanden.");
        }

        else if (payload.length > 1) {
            session.sendErrorMessage("Es kann nur eine Einladung abgelehnt werden.");
        }

        else {

            String user = payload[0];

            if (!session.getInvitations().containsKey(user)) {
                session.sendErrorMessage("User " + user + " hat dich nicht eingeladen.");
            }

            else {
                session.removeInvitation(user);
                session.sendMessage("INVITE_DECLINED:" + user);
            }
        }
    }

    private void importUserdata() {

        final File file = new File(USERDATA_FILE);
        ensureFileExists(file);

        try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                String[] userPass = line.split(";");
                registeredUsers.put(userPass[0], userPass[1]);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportUserdata() {

        final File file = new File(USERDATA_FILE);
        ensureFileExists(file);

        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String user : registeredUsers.keySet()) {
                String pass = registeredUsers.get(user);
                writer.write(user + ";" + pass);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void ensureFileExists(final File file) {
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
