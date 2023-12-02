package com.anonychat.main;

public class ChatJoin extends ChatClient{
	protected String username;
    private String server;
    private int port;
    protected ChatClient client;
    public boolean isRoot; // Flag to determine if the user is a root user

    public ChatJoin(String username, String server, int port, ChatClient client) {
        this.username = username;
        this.server = server;
        this.port = port;
        this.client = client;
    }

    // The initial handshake with the server.
    // It handles various server responses like request for server password, send user name, bans, root, and anonymous.
    public boolean server() {
    	try {
            client.startConnection(server, port); // start a connection with the server

            String serverResponse = client.receiveMessage();
            if ("NOPASSWORD".equals(serverResponse)) {
            	serverResponse = client.receiveMessage();
            } else if ("REQUESTSERVERPASSWORD".equals(serverResponse)) {
            	String serverPassword = Main.getStringInput(" [#]Enter server password: ", true, 1, 77);
                client.sendMessage(serverPassword);

                serverResponse = client.receiveMessage();
                if ("WRONGSERVERPASSWORD".equals(serverResponse)) {
                	Main.clearScreen();
                    System.out.println("[x]Wrong server password. Please try again.");
                    return false;
                }
            } else {
            	System.out.println("[x]Somthing Wrong!!");
                return false;
            }

            // Banned ?
            if ("BANNED".equals(serverResponse)) {
            	Main.clearScreen();
                System.out.println("[x]You are banned from this server.");
                return false;
            }
            
            client.sendMessage(username);
            serverResponse = client.receiveMessage();
            if ("INVALIDUSERNAME".equals(serverResponse)) {
            	Main.clearScreen();
                System.out.println("[x]Invalid Username. Please try again.");
                return false;
            } else if ("INVALIDUSER".equals(serverResponse)) {
            	Main.clearScreen();
            	System.out.println("[x]User already exists. Please try again with a different username.");
            	return false;
            } else if ("INVALIDSTRINGUSERNAME".equals(serverResponse)) {
            	Main.clearScreen();
            	System.out.println("[x]Username must start with a letter. Please try again.");
            	return false;
            } else if ("RESERVEDUSER".equals(serverResponse)) {
            	Main.clearScreen();
            	System.out.println("[x]The chosen username is reserved. Please select a different username.");
            	return false;
            } else if ("REQUESTROOTPASSWORD".equals(serverResponse)) {
                String rootPassword = Main.getStringInput(" [#]Enter root password: ", true, 1, 77);
                client.sendMessage(rootPassword);

                serverResponse = client.receiveMessage();
                if ("WRONGROOTPASSWORD".equals(serverResponse)) {
                	Main.clearScreen();
                    System.out.println("[x]Wrong Cardinality.");
                    return false;
                } else {
                	isRoot = true;
                }
            }
            
            if ("SERVERFULL".equals(serverResponse)) {
            	Main.clearScreen();
                System.out.println("[x]Server Full.");
                return false;
            }
            
            if ("ANONYMOUS".equals(serverResponse)) {
            	String Anonymous = Main.getStringInput("\n > Want to join as Anonymous? (y/n): ", false, 1, 1);
            	client.sendMessage(Anonymous);
            }
            
            // Final check to see if the server returned a successful login response.
            serverResponse = client.receiveMessage();
            return "LOGINPASS".equals(serverResponse);
        } catch (Exception e) {
        	Main.clearScreen();
        	System.out.println("[!]Unable to connect to the server at " + server + ":" + port + 
                    ". Please check your internet connection or try again later.");
            System.out.println("[x]Error connecting to the server.");
            // e.printStackTrace(); // Debugging
            return false;
        }
    }
}