package de.mi.hsrm.chatclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class UserIOService {

    private Client client;

    public UserIOService(Client client) {
        this.client = client;
    }

    public void startUserInput() {

        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;

            do {
                line = reader.readLine();
                interpretUserInput(reader, line);
                if (!client.isChatMenuActive()) {
                    showMenu();
                }

            } while (!line.equals("quit"));
            client.stop();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // interpret user input
    private void interpretUserInput(BufferedReader reader, String line) throws IOException {

        if (client.isChatMenuActive()) {

            // if user disconnects from chat or wants to go back, disable chat
            if (line.equals("!back") || line.equals("!disconnect")) {
                client.setChatMenuActive(false);
            }

            if (line.contains("image")) {
                // image src/main/resources/bildKlein.jpg
                String path = (line.split(" "))[1];
                client.getChatService().prepareSendMessageAndTimer(ChatService.IMAGE_IDENTIFIER, path);
            }

            // else send input as chatmessage
            if (!line.equals("!back")) {
                client.getChatService().prepareSendMessageAndTimer(ChatService.TEXT_IDENTIFIER, line);
            }

            return;
        }

        // if client not in chat
        switch (line.toLowerCase()) {

            case "login":
                client.performLogin(reader);
                break;

            case "register":
                client.performRegistration(reader);
                break;

            case "status":
                client.setStatusMenuActive(true);
                break;

            case "action":
                client.setActionMenuActive(true);
                break;

            case "chat":
                // if a chat connection exists
                if (client.getChatService().getUdpHost() != null && client.getChatService().getUdpPort() != 0) {
                    client.setChatMenuActive(true);
                    showMenu();
                } else {
                    System.out.println(
                            "\nEs gibt grade keine aktive Chatverbindung. Lade jemanden ein oder bestätige eine Chat-Einladung.");
                }
                break;

            case "back":
                client.setChatMenuActive(false);
                client.setActionMenuActive(false);
                client.setStatusMenuActive(false);
                break;

            case "logout":
                client.performLogout();
                break;

            case "invite":
                client.performShowUsers();
                client.performInvite(reader);
                break;

            case "deny":
                client.performShowInvitations();
                client.performDeny(reader);
                break;

            case "accept":
                client.performShowInvitations();
                client.performAccept(reader);
                client.setActionMenuActive(false);
                client.setChatMenuActive(false);
                showMenu();
                break;

            case "users":
                client.performShowUsers();
                break;

            case "invitations":
                client.performShowInvitations();
                break;

            default:
                System.out.println("Die Eingabe scheint fehlerhaft zu sein.");
                break;

        }

    }

    public void showMenu() {

        if (client.isActionMenuActive()) {
            // System.out.println("Variable aktiv");
            showActionMenu();
        } else if (client.isStatusMenuActive()) {
            showStatusMenu();
        } else if (client.isChatMenuActive()) {
            chatUsage();
        } else if (client.isLoggedIn()) {
            showMainMenu();
        } else {
            showStartMenu();
        }

    }

    private void showStartMenu() {
        System.out.println(
                "\nWillkommen in der Chat-Anwendung. Bevor du chatten kannst, musst du dich einloggen oder dich registrieren. Gib dafür eins der untenstehenden Befehle ein.");
        System.out.println("\nregister  - Registriere dich als neuer User.");
        System.out.println("login       - Wenn du dich bereits registriert hast, kannst du dich hier einloggen.");
        System.out.println("quit        - Beenden");
    }

    private void showMainMenu() {
        System.out.println("\nDu bist im Hauptmenü. Was möchstest du tun?");
        System.out.println("\nstatus    - Zum Statusmenü. Hier kannst du Chat-Einladungen und aktive User einsehen.");
        System.out.println("action      - Zum Aktionsmenü. Hier kannst du Einladungen bearbeiten und dich ausloggen.");
        System.out.println("chat        - Zum Chatmenü. Hier kannst du mit anderen Usern chatten.");
        System.out.println("quit        - Beenden");
    }

    private void showStatusMenu() {
        System.out.println("\nDu bist im Statusmenü. Welchen Status möchtest du gerne abfragen?");
        System.out
                .println("\ninvitations   - Hier kannst du alle Einladungen einsehen, die du von Usern erhalten hast.");
        System.out.println("users           - Hier kannst du User sehen, die grade auch eingeloggt sind.");
        System.out.println("back            - Zurück zum Hauptmenü");
        System.out.println("quit            - Beenden");
    }

    private void showActionMenu() {
        System.out.println("\nDu bist im Aktionsmenü. Was möchtest du tun?");
        System.out.println("\ninvite - Lade einen anderen User zum Chat ein.");
        System.out.println("accept - Hier kannst du die Einladung eines anderen Users akzeptieren.");
        System.out.println("deny   - Hier kannst du die Einladung eines anderen Users ablehnen.");
        System.out.println("logout - Ausloggen");
        System.out.println("back   - Zurück zum Hauptmenü");
        System.out.println("quit   - Beenden");
    }

    private void chatUsage() {
        System.out.println("\n Du bist jetzt im Chat-Bereich.");
        System.out.println("\n!back       - Zurück zum Hauptmenü. Du bleibt mit dem Chat im Hintergrund verbunden.");
        System.out.println("!disconnect - Zurück zum Hauptmenü. Die Verbindung zum Chat wird beendet.");
    }

}
