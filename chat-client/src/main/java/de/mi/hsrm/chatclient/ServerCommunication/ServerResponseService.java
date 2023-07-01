package de.mi.hsrm.chatclient.ServerCommunication;

import de.mi.hsrm.chatclient.Client;

public class ServerResponseService {
    
    private Client client;

    public ServerResponseService(Client client) {
        this.client = client;
    }

    public void handleTCPResponse(String response) {
        
        String[] responseElements = response.split(" ");
        String command = responseElements[0];
        String statusCode = responseElements[1];


        String payload = "";
        if (responseElements.length >= 3) {
            payload = responseElements[2];
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
            
            // case "error":
            //     System.out.println(payload.toUpperCase());
            //     break;

            case "invitationsent":
                System.out.println(payload);
                client.getChatService().getReadyForChat();
                break;

            case "INVITE_RESPONSE":

                if (statusCode.equals("1")) {

                    // invite was accepted
                    String[] data = payload.split(";");
                    String host = data[0];
                    int port = Integer.valueOf(data[1]);

                    client.getChatService().setUdpHost(host);
                    client.getChatService().setUdpPort(port);
                    client.getChatService().getReadyForChat();

                } else {
                    // invite was declined
                    System.out.println("\n Eine Einladung wurde abgelehnt.");
                }

                break;

            case "DECLINED":
                System.out.println(payload);
                break;

            default:
                System.out.println("\nCommand nicht gefunden.");
                break;

        }


    }

}
