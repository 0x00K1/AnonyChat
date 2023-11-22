package com.anonychat.main;

import java.io.IOException;

public class Root extends User {

    public Root(String username, String server, int port, ChatClient client) {
        super(username, server, port, client);
    }

    // Handle root commands.
    @Override
    protected void handleCommands(String userInput) throws IOException {
        String[] parts = userInput.split(" ", 2);
        String command = parts[0].toLowerCase();
        String argument = parts.length > 1 ? parts[1] : null;

        switch (command) {
        	case "/help":
        		printHelpMenu();
        		break;
        	case "/broadcast":
	            broadcast(argument);
	            break;
	        case "/anonyme":
	        	AnonyMe();
	        	break;
	        case "/revealme":
	        	RevealMe();
	            break;
	        case "/list":
                handleList();
                break;
            case "/kick":
                handleKick(argument);
                break;
            case "/ban":
                handleBan(argument);
                break;
            case "/unban":
                handleUnban(argument);
                break;
            case "/showban":
                handleShowBanned();
                break;
            case "/clear":
                clearChatHistory();
                break;
            case "/shutdown":
                handleShutdown();
                break;
            case "/exit":
                handleExit();
                break;
            default:
                System.out.println("[x]Unknown admin command: " + userInput + ", Type /help for a list of commands.");
                System.out.print("#" + this.username + "/> ");
                break;
        }
    }
    
    // Prints the help menu with root-level commands.
    @Override
    protected void printHelpMenu() {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                   Administrator Commands                       ║");
        System.out.println("╠════════════════════════════════════════════════════════════════╣");
        System.out.println("║ /help            - Show this help menu                         ║");
        System.out.println("║ /broadcast <msg> - Broadcast a message                         ║");
        System.out.println("║ /anonyMe         - Change your username to 'Anonymous'         ║");
        System.out.println("║ /revealMe        - Return to your real username                ║");
        System.out.println("║ /list            - List all online users                       ║");
        System.out.println("║ /kick <user>     - Kick a user from the chat                   ║");
        System.out.println("║ /ban <user>      - Ban a user from the chat                    ║");
        System.out.println("║ /unban <user>    - Unban a user                                ║");
        System.out.println("║ /showban         - Show all banned users                       ║");
        System.out.println("║ /clear           - Clear the chat history                      ║");
        System.out.println("║ /shutdown        - Shut down the server                        ║");
        System.out.println("║ /exit            - Exit the chat                               ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        
        System.out.print("#" + this.username + "/> ");
    }
    
    // Sends a broadcast message to all users.
    private void broadcast(String message) throws IOException {
        if (message != null && !message.isEmpty()) {
            client.sendMessage("/broadcast " + message);
            System.out.println("Broadcasting message: " + message);
        } else {
            System.out.println("[x]Please provide a message to broadcast.");
        }
        System.out.print("#" + this.username + "/> ");
    }
    
    // Requests the server to list all online users.
    private void handleList() throws IOException {
        client.sendMessage("/list");
        System.out.print("#" + this.username + "/> ");
    }
    
    // Sends a command to the server to kick a specific user.
    private void handleKick(String username) throws IOException {
        if (username != null && !username.isEmpty()) {
            client.sendMessage("/kick " + username);
            System.out.println("Attempting to kick " + username);
        } else {
            System.out.println("[x]Please provide a user to kick.");
        }
        System.out.print("#" + this.username + "/> ");
    }

    // Sends a command to the server to ban a specific user.
    private void handleBan(String username) throws IOException {
        if (username != null && !username.isEmpty()) {
            client.sendMessage("/ban " + username);
            System.out.println("Attempting to ban " + username);
        } else {
            System.out.println("[x]Please provide a user to ban.");
        }
        System.out.print("#" + this.username + "/> ");
    }

    // Sends a command to the server to unbanned a specific user.
    private void handleUnban(String username) throws IOException {
        if (username != null && !username.isEmpty()) {
            client.sendMessage("/unban " + username);
            System.out.println(username + " has been unbanned.");
        } else {
            System.out.println("[x]Please provide a user to unban.");
        }
        System.out.print("#" + this.username + "/> ");
    }
    
    // Requests the server to show all banned users.
    private void handleShowBanned() throws IOException {
        client.sendMessage("/showban");
        System.out.print("#" + this.username + "/> ");
    }
    
    // Sends a command to shut down the server.
    private void handleShutdown() throws IOException {
        System.out.println("Shutting down the server...");
        client.sendMessage("/shutdown");
    }
}