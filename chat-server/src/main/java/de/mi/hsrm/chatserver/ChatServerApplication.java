package de.mi.hsrm.chatserver;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatServerApplication {

	public static void main(String[] args) throws InterruptedException {
        
        Server server = new Server();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop()));

        server.start();
    }

}
