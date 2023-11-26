package com.anonychat.main;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class ChatServer {
	private static String Root;
	private static String RootPassword;
	private static String serverPassword = null;
	private static String savvy;
    private static ServerSocket listener;
	private static int port;
    private static final Map<String, Socket> userSockets = new HashMap<>();
    private static final Map<String, UserSession> sessions = Collections.synchronizedMap(new HashMap<>());
    private static final List<PrintWriter> writers = new ArrayList<>();
    private static final Map<String, PrintWriter> onlineUsers = Collections.synchronizedMap(new HashMap<>());
    private static final Set<String> adminUsers = Collections.synchronizedSet(new HashSet<>());
    private static final Map<String, String> userIPs = new HashMap<>();
    private static final Map<String, List<String>> userIPHistory = new HashMap<>();
    private static final Map<String, String> bannedIPs = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, Long> messageTime = new HashMap<>();
    private static final Set<String> RESERVED_USERNAMES = new HashSet<>(
            Arrays.asList("SERVER", "ANONYMOUS", "BROADCAST")
        );
    
    public static void start() throws IOException {
        
        // Configure the server and start the command-line interface.
    	serverConfig();
    	chatServerCLI();

        // Create a server socket that listens on the specified port.
        listener = new ServerSocket(port);
        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }

    private static class Handler extends Thread {
    	private String username;
    	private boolean Anonymous;
        private Socket socket;
        private String sessionId;
        private BufferedReader in;
        private PrintWriter out;
        private boolean isValid;
        
        public Handler(Socket socket) {
            this.socket = socket;
            this.sessionId = UUID.randomUUID().toString(); // Generate a unique session ID
        }

        public void run() {
            try {
                // Setup I/O streams for the socket.
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                
                // Hello ?
                isValid = authenticator(socket, in, out);
                
                if(!isValid) {
                	socket.close();
                    return;
                }
                
                // User IP && User session.
                String userIP = socket.getInetAddress().getHostAddress();
                UserSession session = new UserSession(username, sessionId, out);
                
                // Welcome :)
                if (!userIPHistory.containsKey(username)) {
                    userIPHistory.put(username, new ArrayList<>());
                }
                sessions.put(sessionId, session);
                userIPs.put(username, userIP);
                onlineUsers.put(username, out);
                userSockets.put(username, socket);
                writers.add(out);
                userIPHistory.get(username).add(userIP);
                
                // Anonymous ? 
                if(Anonymous) AnonyMe();

                for (PrintWriter writer : writers) {
                    writer.println("MESSAGE [SERVER]> " + username + " has joined");
                }

                // Handle the incoming messages.
                final int MAX_MESSAGE_LENGTH = 50; // 50 Good for our CLI
                final long MIN_TIME_BETWEEN_MESSAGES = 1000; // 1s
                while (true){
                	
                	// DCheck if the user IP is banned.
            		if (bannedIPs.containsKey(userIP)) {
            			out.println("BANNED");
            			return;
            		}
            		
                	String input = in.readLine(); // Read user message.
                	UserSession currentUserSession = sessions.get(sessionId); // Get user session.
                	
                    if (input == null || input.trim().isEmpty()) continue;
                    
                    // NO SPAM.
                    long currentTime = System.currentTimeMillis();
                    Long lastMessageTime = messageTime.getOrDefault(currentUserSession.getRealUsername(), 0L);
                    if (currentTime - lastMessageTime < MIN_TIME_BETWEEN_MESSAGES) {
                        out.println("MESSAGE [SERVER]> Don't SPAM.");
                        continue;
                    }
                    messageTime.put(currentUserSession.getRealUsername(), currentTime);
                    
                    // Length check.
                    if (input.length() > MAX_MESSAGE_LENGTH) {
                        out.println("MESSAGE [SERVER]> Your message is too long. Maximum " + MAX_MESSAGE_LENGTH + " characters.");
                        continue;
                    }
                    
                    if (input.startsWith("/")) {
                        if (adminUsers.contains(currentUserSession.getRealUsername())) {
                            handleRootCommand(input);
                        } else {
                            handleUserCommand(input);
                        }
                    } else {
                        for (String user : onlineUsers.keySet()) {
                            PrintWriter writer = onlineUsers.get(user);
                            
                            if (Root.equals(user) && "Anonymous".equals(username)) {
                                writer.println("MESSAGE [" + currentUserSession.getRealUsername() + "]> " + input);
                            } else {
                                writer.println("MESSAGE [" + username + "]> " + input);
                            }
                        }
                    }
                }
            } catch (SocketException e) {
            	System.out.println("[#]Server Refresh . .");
            } catch (IOException e) {
            	System.out.println("[x]IOError.\nDebug:\n" + e);
            } finally {
                if (username != null && out != null) {
                	if (isValid) {
                		// Bye.
                		for (PrintWriter writer : writers) {
                        	writer.println("MESSAGE [SERVER]> " + username + " has left");
                        }
                		RevealMe();
                		sessions.remove(sessionId);
                		userSockets.remove(username);
                        userIPs.remove(username);
                    	writers.remove(out);
                        onlineUsers.remove(username);
                	}
                }
                try {
                    socket.close();
                } catch (SocketException e) {
                	System.out.println("[#]Server Closed.");
                } catch (IOException e) {
                	System.out.println("[x]Something Error.");	
                }
            }
        }
        
        private boolean authenticator(Socket socket, BufferedReader in, PrintWriter out) {
        	String userIP = socket.getInetAddress().getHostAddress();
        	boolean Rooter = false;
        	try {
        		if (serverPassword == null || serverPassword.trim().isEmpty()) {
        			out.println("NOPASSWORD");
        		} else {
        			out.println("REQUESTSERVERPASSWORD");
        			String clientServerPassword = in.readLine();
        			if (clientServerPassword == null || !SecurityUtils.validatePassword(clientServerPassword, savvy, serverPassword)) {
        				out.println("WRONGSERVERPASSWORD");
        				return false;
        			}
        		}
               
        		// Check if the user IP is banned.
        		if (bannedIPs.containsKey(userIP)) {
        			out.println("BANNED");
        			return false;
        		}
               
        		out.println("REQUESTUSERNAME");
        		String GETusername = in.readLine();
        		if (GETusername == null || GETusername.trim().isEmpty()) {
        			out.println("INVALIDUSERNAME");
        			return false;
        		} else if (onlineUsers.containsKey(GETusername)) {
        			out.println("INVALIDUSER");
        			return false;
        		} else if (!Character.isLetter(GETusername.charAt(0))) {
                    out.println("INVALIDSTRINGUSERNAME");
        			return false;
        		} else if (isUsernameReserved(GETusername)) {
        			out.println("RESERVEDUSER");
        			return false;
        		}
        		GETusername = GETusername.trim();

        		if (Root.equalsIgnoreCase(GETusername)) {
        			out.println("REQUESTROOTPASSWORD");
        			String clientRootPassword = in.readLine();
        			if (clientRootPassword == null || !SecurityUtils.validatePassword(clientRootPassword, savvy,  RootPassword)) {
        				out.println("WRONGROOTPASSWORD");
        				return false;
        			}
        			Rooter = true;
        		}
               
        		username = GETusername;
        		if(Rooter) adminUsers.add(username);
        		
        		// AnonyMe
        		out.println("ANONYMOUS");
        		String isAnony = in.readLine();
        		if("y".equalsIgnoreCase(isAnony)) Anonymous = true;
        		
        		out.println("LOGINPASS");
        		return true;
        	} catch (IOException e) {
        		e.printStackTrace();
        		return false;
        	}
        }
        
        private void handleRootCommand(String command) {
            String[] parts = command.split(" ", 2);
            String cmd = parts[0].toLowerCase();
            String argument = parts.length > 1 ? parts[1] : null;

            switch (cmd) {
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
                    listOnlineUsers();
                    break;
            	case "/kick":
                    kickUser(argument);
                    break;
            	case "/ban":
                    banUser(argument);
                    break;
                case "/unban":
                    unbanUser(argument);
                    break;
                case "/showban":
                    showBannedUsers();
                    break;
                case "/shutdown":
                    shutdownServer();
                    break;
                default:
                    out.println("MESSAGE [SERVER]> Unknown command.");
                    break;
            }
        }
        
        private void handleUserCommand(String command) {
            String[] parts = command.split(" ", 2);
            String cmd = parts[0].toLowerCase();

            switch (cmd) {
                case "/anonyme":
                	AnonyMe();
                	break;
                case "/revealme":
                	RevealMe();
                    break;
                default:
                    out.println("MESSAGE [SERVER]> Unknown command.");
                    break;
            }
        }
        
        private void broadcast(String message) {
            if (message != null) {
                for (PrintWriter writer : writers) {
                    writer.println("MESSAGE [BROADCAST]> " + message);
                }
            } else {
                out.println("MESSAGE [SERVER]> Please provide a message.");
            }
        }
        
        private void AnonyMe() {
            UserSession session = sessions.get(sessionId);
            if (!"Anonymous".equals(username)) {
                session.setRealUsername(username);
                username = "Anonymous";
                out.println("MESSAGE [SERVER]> Anonymous mode activated!");
            } else {
                out.println("MESSAGE [SERVER]> You are already anonymized.");
            }
        }

        private void RevealMe() {
            UserSession session = sessions.get(sessionId);
            if ("Anonymous".equals(username)) {
                username = session.getRealUsername();
                out.println("MESSAGE [SERVER]> Your username is now back to " + username);
            } else {
                out.println("MESSAGE [SERVER]> You are not currently anonymized.");
            }
        }
        
        private void listOnlineUsers() {
            out.println("MESSAGE [SERVER]> Online users: " + String.join(", ", onlineUsers.keySet()));
        }

        private void kickUser(String username) {
            if (username != null && onlineUsers.containsKey(username) && !adminUsers.contains(username)) {
                PrintWriter writer = onlineUsers.get(username);
                writer.println("MESSAGE [SERVER]> You have been kicked from this chat.  Type '/exit' to exit");
                writer.println("KICKED");
                writers.remove(writer);
                onlineUsers.remove(username);
                out.println("MESSAGE [SERVER]> " + username + " has been kicked.");

                // Close the users socket connection.
                try {
                    userSockets.get(username).close();
                } catch (IOException e) {
                    System.out.println("[x]Error closing socket for kicked user: " + e.getMessage());
                }
                userSockets.remove(username);
            } else {
                out.println("MESSAGE [SERVER]> Please provide a valid username.");
            }
        }
        
        private void banUser(String username) {
            if (username != null && onlineUsers.containsKey(username) && !adminUsers.contains(username)) {
                String userIP = userIPs.get(username);
                bannedIPs.put(userIP, username);
                PrintWriter writer = onlineUsers.get(username);
                writer.println("MESSAGE [SERVER]> You have been banned from this chat.  Type '/exit' to exit.");
                writer.println("BANNED");
                writers.remove(writer);
                onlineUsers.remove(username);
                out.println("MESSAGE [SERVER]> " + username + " has been banned.");

                // Close the users socket connection.
                try {
                    userSockets.get(username).close();
                } catch (IOException e) {
                    System.out.println("[x]Error closing socket for banned user: " + e.getMessage());
                }
                userSockets.remove(username);
            } else {
                out.println("MESSAGE [SERVER]> Please provide a valid username.");
            }
        }

        private void unbanUser(String username) {
            if (username != null && !adminUsers.contains(username)) {
                List<String> userIPsToUnban = userIPHistory.get(username);
                if (userIPsToUnban != null) {
                    for (String ip : userIPsToUnban) {
                        if (bannedIPs.containsKey(ip)) {
                            bannedIPs.remove(ip);
                        }
                    }
                    out.println("MESSAGE [SERVER]> " + username + " has been unbanned.");
                } else {
                    out.println("MESSAGE [SERVER]> No IP history found for username.");
                }
            } else {
                out.println("MESSAGE [SERVER]> Please provide a valid username or an unprivileged username.");
            }
        }
        
        private void showBannedUsers() {
            if (bannedIPs.isEmpty()) {
                out.println("MESSAGE [SERVER]> No users are currently banned.");
            } else {
            	out.println("MESSAGE [SERVER]> Banned users: " + String.join(", ", bannedIPs.values()));
            }
        }
        
        private void shutdownServer() {
            try {
                for (PrintWriter writer : writers) {
                    writer.println("MESSAGE [SERVER]> Server Down.  Type '/exit' to exit.");
                }
                
                listener.close();
                System.exit(0);
            } catch (IOException e) {
                out.println("MESSAGE [SERVER]> An error occurred while trying to shut down the server.");
            }
        }
    }
    
    // Manage Sessions
    private static class UserSession {
        private String realUsername;
        @SuppressWarnings("unused")
		private String username;
        @SuppressWarnings("unused")
        private String sessionId;
        @SuppressWarnings("unused")
        private PrintWriter out;

        public UserSession(String username, String sessionId, PrintWriter out) {
            this.realUsername = username;
        	this.username = username;
            this.sessionId = sessionId;
            this.out = out;
        }

        // Accessory 
        public String getRealUsername() {
            return realUsername;
        }

        // Mutator
        public void setRealUsername(String realUsername) {
            this.realUsername = realUsername;
        }
    }

    
    // Security && Hashing
    private class SecurityUtils {

        public static String generateSalt() {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            return Base64.getEncoder().encodeToString(salt);
        }

        public static String hashPassword(String password, String salt) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.reset();
                digest.update(Base64.getDecoder().decode(salt));
                byte[] hash = digest.digest(password.getBytes());
                return Base64.getEncoder().encodeToString(hash);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("[x]Error hashing password", e);
            }
        }

        public static boolean validatePassword(String password, String salt, String expectedHash) {
            String hashed = hashPassword(password, salt);
            return hashed.equals(expectedHash);
        }
    }
    
    private static void printLocalIpAddresses() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            // Filters out 127.0.0.1 and inactive interfaces
            if (!networkInterface.isLoopback() && networkInterface.isUp()) {
                for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                    InetAddress address = interfaceAddress.getAddress();
                    if (address instanceof Inet4Address) {
                        System.out.println("  [IP:PORT] - - > " + address.getHostAddress() + ":" + port);
                    }
                }
            }
        }
    }
    
    private static void serverConfig() {
        Main.clearScreen();

        System.out.println("╔═════════════  Server Configuration  ═════════════╗");
        port = Main.getIntInput("\n > Enter server port (1024-65535): ", 1024, 65535, false);

        if ("y".equalsIgnoreCase(Main.getStringInput("\n > Want to set a server password? (y/n): ", false, 1, 1))) {
            serverPassword = Main.getStringInput(" > Set password (4-77 chars): ", true, 4, 77);
        }
        
        String newRootUsername;
        do {
            newRootUsername = Main.getStringInput("\n > Enter username for root (3-10 chars): ", false, 3, 10);
            if (!Character.isLetter(newRootUsername.charAt(0))) {
            	Main.clearScreen();
                System.out.println("[x]Username must start with a letter. Please try again.");
            } else if (isUsernameReserved(newRootUsername)) {
            	Main.clearScreen();
            	System.out.println("[x]The chosen username is reserved. Please select a different username.");
    		}
        } while (!Character.isLetter(newRootUsername.charAt(0)) || isUsernameReserved(newRootUsername));
        String newRootPassword = Main.getStringInput(" > Enter password for root (10-77 chars): ", true, 10, 77);

        String salt = SecurityUtils.generateSalt();
        String hashedServerPassword = null;
        if (serverPassword != null) {
            hashedServerPassword = SecurityUtils.hashPassword(serverPassword, salt);
        }
        String hashedRootPassword = SecurityUtils.hashPassword(newRootPassword, salt);
        
        Root = newRootUsername;
        RootPassword = hashedRootPassword;
        serverPassword = hashedServerPassword;
        savvy = salt;
    }
    
    private static boolean isUsernameReserved(String username) {
        for (String reserved : RESERVED_USERNAMES) {
            if (reserved.equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }
    
    public static void chatServerCLI() throws SocketException {
        Main.clearScreen();

        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║" + Main.padCenter(Main.PROGRAM_NAME + " Server", 48) + "║");
        System.out.println("╚════════════════════════════════════════════════╝");

        System.out.println("\n > Server running on . .\n");
        printLocalIpAddresses(); // Print the local IP address

        System.out.println("\n\n > Root Account: " + Root);

        System.out.println("\n > Happy Chatting :)\n\n");
    }
}