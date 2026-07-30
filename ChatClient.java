package com.zhaoxuchun.chat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public final class ChatClient {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private ChatClient() {
    }

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 9090;

        try (Scanner scanner = new Scanner(System.in);
             Socket socket = new Socket(host, port);
             ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
            output.flush();
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            send(output, ChatMessage.of(MessageType.LOGIN, username, null, ""));

            Thread reader = Thread.ofVirtual().start(() -> readMessages(input));
            printHelp();

            while (scanner.hasNextLine()) {
                String command = scanner.nextLine().trim();
                if (command.equals("/quit")) {
                    send(output, ChatMessage.of(MessageType.QUIT, username, null, ""));
                    break;
                }
                if (command.equals("/users")) {
                    send(output, ChatMessage.of(MessageType.USER_LIST, username, null, ""));
                } else if (command.startsWith("/all ")) {
                    send(output, ChatMessage.of(
                            MessageType.GROUP_CHAT,
                            username,
                            null,
                            command.substring(5)
                    ));
                } else if (command.startsWith("/to ")) {
                    String[] parts = command.split("\\s+", 3);
                    if (parts.length < 3) {
                        System.out.println("Usage: /to <username> <message>");
                    } else {
                        send(output, ChatMessage.of(
                                MessageType.PRIVATE_CHAT,
                                username,
                                parts[1],
                                parts[2]
                        ));
                    }
                } else {
                    printHelp();
                }
            }
            reader.interrupt();
        }
    }

    private static synchronized void send(ObjectOutputStream output, ChatMessage message) {
        try {
            output.writeObject(message);
            output.flush();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to send message.", exception);
        }
    }

    private static void readMessages(ObjectInputStream input) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                ChatMessage message = (ChatMessage) input.readObject();
                String time = message.sentAt().format(TIME_FORMAT);
                if (message.type() == MessageType.USER_LIST) {
                    System.out.printf("[%s] Online: %s%n", time, message.content());
                } else if (message.type() == MessageType.PRIVATE_CHAT) {
                    System.out.printf("[%s] [private] %s: %s%n", time, message.sender(), message.content());
                } else {
                    System.out.printf("[%s] %s: %s%n", time, message.sender(), message.content());
                }
            }
        } catch (IOException | ClassNotFoundException exception) {
            if (!Thread.currentThread().isInterrupted()) {
                System.out.println("Connection closed.");
            }
        }
    }

    private static void printHelp() {
        System.out.println("Commands: /all <message>, /to <user> <message>, /users, /quit");
    }
}
