package de.mi.hsrm.chatclient.ServerCommunication;

import de.mi.hsrm.chatclient.Client;

public class ServerResponseHandler {
    
    private Client client;

    public ServerResponseHandler(Client client) {
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

            default:
                System.out.println("\nCommand nicht gefunden.");
                break;

        }


    }

}
