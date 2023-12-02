package com.anonychat.main;

import java.io.Console;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.UUID;

public class Main {
    public static final String PROGRAM_NAME = "AnonyChat";
    private static final Scanner SCANNER = new Scanner(System.in);
    public static final Console CONSOLE = System.console();


    public static void main(String[] args) throws IOException {
    	
        // Check to ensure the program is running in a CLI environment.
    	if (!RunCLI()) {
            System.err.println("This program is restricted to run only in command prompt(CMD) or terminal.");
            System.exit(1);
        }
    	
        int option;
        do {
            displayMenu();
            option = getIntInput("\n#-->: ", 0, 2, true);

            switch (option) {
                case 1:
                    ChatServer.start();
                    break;
                case 2:
                    handleJoin();
                    break;
                case 0x00:
                    clearScreen();
                    System.out.println("Exiting program . .");
                    break;
                default:
                    continue;
            }
        } while (option != 0);

        // Bye.
        SCANNER.close();
        System.out.println("Thank you for using " + PROGRAM_NAME + ".");
    }
    
    // Handles the logic when a user selects the option to join a server.
    private static void handleJoin() throws IOException {
        clearScreen();

        System.out.println("╔═════════════  Server Join  ═════════════╗");

        String server = getStringInput("\n > Enter server address: ", false, 1, 99);
        int port = getIntInput(" > Enter server port (1024-65535): ", 1024, 65535, false);
        String username = getStringInput("\n > Enter username (3-10 chars): ", false, 3, 10);
        if (!Character.isLetter(username.charAt(0))) {
        	Main.clearScreen();
            System.out.println("[x]Username must start with a letter. Please try again.");
            back();
            return;
        }

        // Code Check.
        if (!verifyCode()) {
        	clearScreen();
            System.out.println("[x]Code mismatch. Please try again.");
            back();
            return;
        }

        // Welcome.
        ChatClient client = new ChatClient();
        ChatJoin join = new ChatJoin(username, server, port, client);

        if (join.server()) {
            if (join.isRoot) // SavvyRooter
            	handleRootLogin(username, server, port, client, join);
            else
            	handleUserLogin(username, server, port, client, join);
        } else {
            System.out.println("[x]Login failed.");
            back();
            return;
        }
    }

    // Logic for logging in as a "root" user, which may have additional privileges.
    private static void handleRootLogin(String username, String server, int port, ChatClient client, ChatJoin join) throws IOException {
        clearScreen();
        join = new Root(username, server, port, client);
        try {
            ((Root) join).enterChat();
        } catch (IOException e) {
            System.out.println("[x]Join failed.");
            back();
        }
    }

    // Logic for logging in as a regular user.
    private static void handleUserLogin(String username, String server, int port, ChatClient client, ChatJoin join) throws IOException {
        clearScreen();
        join = new User(username, server, port, client);
        try {
            ((User) join).enterChat();
        } catch (IOException e) {
            System.out.println("[x]Join failed.");
            back();
        }
    }

    /*
     *  	Validation Area && Happy User Interface :)
     *  	. . . . . . . . . . . . . . . . . . . . . 
     */
    
    public static boolean RunCLI() {
        return System.console() != null;
    }
    
    public static void displayMenu() {
        clearScreen();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                              ║");
        System.out.println("║" + padCenter("Welcome to the " + PROGRAM_NAME, 62) + "║");
        System.out.println("║                                                              ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║    1. Start " + padRight(PROGRAM_NAME + " Server.", 49) + "║");
        System.out.println("║    2. Join  " + padRight(PROGRAM_NAME + " Server.", 49) + "║");
        System.out.println("║    0. Exit                                                   ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }

    public static void lineInterface() {
    	System.out.println(new String(new char[65]).replace('\0', '═'));
    }
    
    public static String padRight(String text, int length) {
        return String.format("%-" + length + "s", text);
    }

    public static String padCenter(String text, int length) {
        int paddingSize = (length - text.length()) / 2;
        String padding = new String(new char[paddingSize]).replace('\0', ' ');
        return padding + text + padding;
    }
    
    public static void back() {
        @SuppressWarnings("resource")
		Scanner back = new Scanner(System.in);
        System.out.println("\n\n[*]Press [ENTER] to go Back . .");
        back.nextLine();
        // back.close(); // DONT CLOSE IT
        clearScreen();
    }

    public static void clearScreen() {
        try {
            String operatingSystem = System.getProperty("os.name"); // Check the current OS

            if (operatingSystem.contains("Windows")) {
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "cls");
                pb.inheritIO();
                Process process = pb.start();
                process.waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();

                try {
                    ProcessBuilder pbClear = new ProcessBuilder("clear");
                    pbClear.inheritIO();
                    Process processClear = pbClear.start();
                    processClear.waitFor();
                } catch (Exception ex) {
                    // If an error occurs with the clear command, it will be ignored since the escape sequences should have cleared the screen.
                }
            }
        } catch (Exception e) {
            System.err.println("[x]Error clearing screen: " + e.getMessage());
            System.exit(1); // Don't let the user run the program without use clearScreen method
        }
    }
    
    public static boolean verifyCode() throws IOException {
        String randomCode = UUID.randomUUID().toString().substring(0, 6);
        String userCode = getStringInput("\n [#]Enter the code for verification: " + randomCode + "\n  #-- >: ", false, 1, 38);
        return randomCode.equals(userCode);
    }

    public static boolean validateString(String input, int minLen, int maxLen) {
        if (input.length() > maxLen || input.length() < minLen) {
        	clearScreen();
            System.out.println("[x]Input must be equal or between " + minLen + " and " + maxLen + " characters.");
            lineInterface();
            return false;
        }
        return true;
    }

    public static String getStringInput(String prompt, boolean isPassword, int minLen, int maxLen) {
        String input = null;
        while (true) {
            System.out.print(prompt);

            // Use Console.readPassword() if it is a password input, otherwise use the scanner
            if (isPassword) {
                if (CONSOLE != null) {
                    char[] passwordArray = CONSOLE.readPassword();
                    input = new String(passwordArray);
                    // Zero out the password array immediately after use
                    Arrays.fill(passwordArray, ' ');
                } else {
                    System.err.println("[x]Console not available.");
                    System.exit(1);
                }
            } else {
                input = SCANNER.next();
            }

            if (validateString(input, minLen, maxLen)) {
                break;
            }
            back();
        }
        return input;
    }

    public static boolean validateInt(int input, int minValue, int maxValue) {
        if (input < minValue || input > maxValue) {
        	clearScreen();
            System.out.println("[x]Invalid input. Please enter a value equal or between " + minValue + " and " + maxValue + " as integers.");
            lineInterface();
            return false;
        }
        return true;
    }

    public static int getIntInput(String prompt, int minValue, int maxValue, boolean rect) {
		int input = -1; // Don't change this value. (switch.case must not equal the default value)
        while (true) {
            System.out.print(prompt);
            try {
                input = SCANNER.nextInt();
                if (validateInt(input, minValue, maxValue)) {
                    break;
                }
            } catch (java.util.InputMismatchException e) {
            	clearScreen();
                System.out.println("[!]Please input a numeric value.");
                System.out.println("[x]OverFlow Input.");
                lineInterface();
                SCANNER.nextLine(); // clear buffer
            }
            back();
            if(rect)
            	break;
            continue;
        }
        return input;
    }
}