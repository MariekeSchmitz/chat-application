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
                System.out.println("------------------------------------------");
                System.out.println("Registrierung erfolgreich. Du kannst dich jetzt einloggen.");
                System.out.println("------------------------------------------");
                break;

            default:
                System.out.println("Command nicht gefunden.");
                break;

        }


    }

}
