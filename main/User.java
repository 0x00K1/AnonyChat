package com.anonychat.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class User extends ChatJoin{
    private List<String> chatHistory = new ArrayList<>();
    protected StringBuilder userInput = new StringBuilder();
    protected String currentInput = userInput.toString();

    public User(String username, String server, int port, ChatClient client) {
        super(username, server, port, client);
    }
    
    // Handles the user's entry into the chat.
    public void enterChat() throws IOException {
        client.stdIn = new BufferedReader(new InputStreamReader(System.in));

        Thread listenerThread = new Thread(() -> {
            try {
                while (true) {
                    String message = client.receiveMessage();
                    if (message != null) {
                        if (message.startsWith("MESSAGE")) {
                            String formattedMessage = message.substring(8);
                            chatHistory.add(formattedMessage);
                            printChatInterface(currentInput);
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

        // The main loop for reading user input from the console.
        while (true) {
            if (client.stdIn.ready()) {
                char inputChar = (char) client.stdIn.read();
                if (inputChar == '\n') {
                    handleInput(userInput.toString());
                    userInput.setLength(0);
                } else if (inputChar == '\r') {
                    // Ignore carriage return
                } else {
                    userInput.append(inputChar);
                }
            }
        }
    }
    
    // Reprints the user's current input line, typically called after the chat interface is updated.
    private void reprintUserInput(String currentInput) {
        System.out.print("\033[2K"); // Clear the current line
        System.out.print("\r#" + this.username + "/> " + currentInput);
        System.out.flush();
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
    protected void printChatInterface(String currentInput) {
        Main.clearScreen();
        
        Main.clearScreen();
        System.out.println(" [SERVER]> Welcome " + this.username + ",  Type '/help' for available commands.");
        
        System.out.println("╔═════════════════════════════════════════════════════════════════");
        for (String message : chatHistory) {
            System.out.println("║  " + message);
        }
        System.out.println("╚═════════════════════════════════════════════════════════════════");
        reprintUserInput(currentInput);
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
            	AnonyMe();
            	break;
            case "/revealme":
            	RevealMe();
                break;
            case "/clear":
                clearChatHistory();
                break;
            case "/exit":
            	handleExit();
                break;
            default:
                System.out.println("[x]Unknown user command: " + userInput + ", Type /help for a list of commands.");
                System.out.print("#" + this.username + "/> ");
                break;
        }
    }
    
    // Prints the help menu with user-level commands.
    protected void printHelpMenu() { 
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                         User Commands                          ║");
        System.out.println("╠════════════════════════════════════════════════════════════════╣");
        System.out.println("║ /help       - Show this help menu                              ║");
        System.out.println("║ /anonyMe    - Change username to Anonymous                     ║");
        System.out.println("║ /revealMe   - Return to your real username                     ║");
        System.out.println("║ /clear      - Clear the chat history                           ║");
        System.out.println("║ /exit       - Exit the chat                                    ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");

        System.out.print("#" + this.username + "/> ");
    }
    
    // Sends a command to change the user to anonymous mode.
    protected void AnonyMe() throws IOException {
        client.sendMessage("/AnonyMe");
    }
    
    // Sends a command to reveal the user from anonymous mode.
    protected void RevealMe() throws IOException {
        client.sendMessage("/RevealMe");
    }
 
    // Clears the chat history from the user's view.
    protected void clearChatHistory() {
        chatHistory.clear();
        printChatInterface("");
    }
    
    protected void handleExit() {
    	Main.clearScreen();
        System.out.println("Exiting program . .");
        System.out.println("Thank you for using " + Main.PROGRAM_NAME + ".");
        System.exit(0);
    }
}