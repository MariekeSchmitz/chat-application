package de.mi.hsrm.chatclient;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatClientApplication {

	public static void main(String[] args) {
		
		String host;
		int port;

		Client client;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {

			System.out.println("Host eingeben oder Enter für default-Host  " + Client.DEFAULT_TCP_HOST);
			String input = reader.readLine();

			if (input == null || input.trim().equals("")) {
				host = Client.DEFAULT_TCP_HOST;
			} else {
				host = input;
			}
	
	
			System.out.println("Port eingeben oder Enter für default-Port " + Client.DEFAULT_TCP_PORT);
			input = reader.readLine();
	
			if (input == null || input.trim().equals("")) {
				port = Client.DEFAULT_TCP_PORT;
			} else {
				port = Integer.valueOf(input);
			}
	
			client = new Client(host, port);
			client.start();

		} catch (Exception e) {
			e.printStackTrace();
		}	

	}

}
