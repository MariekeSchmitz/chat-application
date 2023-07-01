package de.mi.hsrm.chatclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatClientApplication {

	public static void main(String[] args) {
		
		String input;
		String host;
		int port;

		Client client;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {

			System.out.println("Host eingeben oder Enter für default-Host  " + Client.DEFAULT_TCP_HOST + "):");
			input = reader.readLine();

			if (input == null || input.trim().equals("")) {
				host = Client.DEFAULT_TCP_HOST;
			} else {
				host = input;
			}
	
	
			System.out.println("Port eingeben oder Enter für default-Port " + Client.DEFAULT_TCP_PORT + "):");
			input = reader.readLine();
	
			if (input == null || input.trim().equals("")) {
				port = Client.DEFAULT_TCP_PORT;
			} else {
				host = Integer.valueOf(input);
			}
	
			client = new Client(host, port);
			client.start();

		} catch (Exception e) {
			e.printStackTrace();
		}	

	}

}
