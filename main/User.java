package com.anonychat.main;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class User extends ChatJoin{
    private List<String> chatHistory = new ArrayList<>();

    public User(String username, String server, int port, ChatClient client) {
        super(username, server, port, client);
    }
    
    // Handles the user's entry into the chat.
    public void enterChat() throws IOException {
        if (Main.CONSOLE == null) {
            System.out.println("[x]No console available. Exiting program.");
            return;
        }

        Thread listenerThread = new Thread(() -> {
            try {
                while (true) {
                    String message = client.receiveMessage();
                    if (message != null) {
                        if (message.startsWith("MESSAGE")) {
                            String formattedMessage = message.substring(8);
                            chatHistory.add(formattedMessage);
                            printChatInterface(false);
                        } else if(message.startsWith("BANNED")) {
                        	client.closeConnection();
                        }
                    }
                }
            } catch (SocketException e) {
            	System.out.println("[#]Server Closed.");
            } catch (IOException e) {
            	System.out.println("[x]Something Error.");	
            }
        });
        listenerThread.start();

        String userInput;
        while (true) {

        	// Use readPassword method which does not echo the input on the console for privacy.
            // This way, the messages typed by the user are not visible to on lookers,
            // ensuring privacy even in a shared environment.
            char[] inputArray = Main.CONSOLE.readPassword("#" + this.username + "/> ");

            userInput = new String(inputArray);

            if (userInput != null && !userInput.trim().isEmpty()) {
                handleInput(userInput);
            }
        }
    }
     
    // Processes the user input, checking if it's a command or a regular message.
    protected void handleInput(String userInput) throws IOException {
        if (userInput.trim().startsWith("/")) {
        	handleCommands(userInput);
        } else {
            client.sendMessage(userInput);
        }
    }
    
    // Prints the chat interface with the chat history and current input.
    protected void printChatInterface(boolean up) {
        Main.clearScreen();
        
        Main.clearScreen();
        System.out.println(" [SERVER]> Welcome " + this.username + ",  Type '/help' for available commands.");
        
        System.out.println("╔═════════════════════════════════════════════════════════════════");
        for (String message : chatHistory) {
            System.out.println("║  " + message);
        }
        System.out.println("╚═════════════════════════════════════════════════════════════════");
        if(!up) System.out.print("#" + this.username + "/> ");
    }
    
    // Handle user commands.
    protected void handleCommands(String userInput) throws IOException {
        String[] parts = userInput.split(" ", 2);
        String command = parts[0].toLowerCase();
        
        switch (command) {
            case "/help":
                printHelpMenu();
                break;
            case "/anonyme":
            	handleAnonyMe();
            	break;
            case "/revealme":
            	handleRevealMe();
                break;
            case "/clear":
            	handleClear();
                break;
            case "/exit":
            	handleExit();
                break;
            default:
                System.out.println("[x]Unknown user command: " + userInput + ", Type /help for a list of commands.");
                break;
        }
    }
    
    // Prints the help menu with user-level commands.
    protected void printHelpMenu() { 
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                         User Commands                          ║");
        System.out.println("╠════════════════════════════════════════════════════════════════╣");
        System.out.println("║ /help       - Show this help menu                              ║");
        System.out.println("║ /AnonyMe    - Change username to Anonymous                     ║");
        System.out.println("║ /RevealMe   - Return to your real username                     ║");
        System.out.println("║ /clear      - Clear the chat history                           ║");
        System.out.println("║ /exit       - Exit the chat                                    ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
    }
    
    // Sends a command to change the user to anonymous mode.
    protected void handleAnonyMe() throws IOException {
        client.sendMessage("/AnonyMe");
    }
    
    // Sends a command to reveal the user from anonymous mode.
    protected void handleRevealMe() throws IOException {
        client.sendMessage("/RevealMe");
    }
 
    // Clears the chat history from the user's view.
    protected void handleClear() {
        chatHistory.clear();
        printChatInterface(true);
    }
    
    protected void handleExit() {
    	Main.clearScreen();
        System.out.println("Exiting program . .");
        System.out.println("Thank you for using " + Main.PROGRAM_NAME + ".");
        System.exit(0);
    }
}