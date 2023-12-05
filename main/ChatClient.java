package com.anonychat.main;

import java.io.*;
import java.net.*;

public class ChatClient {
    private Socket socket; // The client's socket
    private BufferedReader in; // Reader to receive messages from the server
    private PrintWriter out; // Writer to send messages to the server

    public void startConnection(String ip, int port) throws IOException {
        // Establishes a connection to the server at the given IP address and port.
        socket = new Socket(ip, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    public String receiveMessage() throws IOException {
        // Receives a message from the server.
        if (in != null) {
            return in.readLine();
        } else {
            // If the BufferedReader is not initialized, a connection has not been established.
            System.out.println("[x]Connection is not established. Reader is null.");
            return null;
        }
    }
    
    public void sendMessage(String msg) throws IOException {
        // Sends a message to the server.
        if (out != null) {
            out.println(msg);
        } else {
            // If the PrintWriter is not initialized, a connection has not been established.
            System.out.println("[x]Connection is not established. Writer is null.");
        }
    }
    
    public void closeConnection() throws IOException {
    	Main.clearScreen(); if (out != null) out.close(); if (in != null) in.close(); if (socket != null) socket.close(); System.exit(0);
	}
}