package de.mi.hsrm.chatclient.ServerService;

import de.mi.hsrm.chatclient.Client;

public class ServerResponseService {
    
    private Client client;

    public ServerResponseService(Client client) {
        this.client = client;
    }

    public void handleTCPResponse(String response) {
        
        String[] responseElements = response.split(":");
        String command = responseElements[0];

        String payload = "";
        if (responseElements.length >= 2) {
            payload = responseElements[1];
        } 

        switch(command.toUpperCase()) {

            case "REGISTER":
                System.out.println("\nRegistrierung erfolgreich. Du kannst dich jetzt einloggen.");
                break;

            case "LOGIN":
                client.setLoggedIn(true);
                System.out.println("\nLogin erfolgreich. Du bist jetzt eingeloggt.");
                break;

            case "REQUEST_ACTIVES":
                System.out.println("\nAktuell sind diese User aktiv:");

                for (String username: payload.split(";")) {
                    System.out.println("- " + username);
                }

                break;

            case "SHOW_INVITES":
                System.out.println("\nDu wurdest von diesen Usern eingeladen:");

                 for (String username: payload.split(";")) {
                    System.out.println("- " + username);
                }

                break;
            
            case "ERROR":
                System.out.println(payload.toUpperCase());
                break;

            case "INVITE_SENT":
                System.out.println(payload);
                client.getChatService().getReadyForChat();
                break;

            case "INVITE_ACCEPT":
                System.out.println("Invitation wurde angenommen");
                String[] data = payload.split(";");
                String host = data[0];
                int port = Integer.valueOf(data[1]);

                System.out.println("Host: " + host);
                System.out.println("Port: " + port);

                client.getChatService().setUdpHost(host);
                client.getChatService().setUdpPort(port);
                client.getChatService().getReadyForChat();
           
            case "INVITE_DECLINED":
                System.out.println(payload);
                break;

            default:
                System.out.println("\nCommand nicht gefunden.");
                break;

        }


    }

}
