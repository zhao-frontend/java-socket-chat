package com.zhaoxuchun.chat;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatServer {
    private final int port;
    private final Map<String, ClientSession> sessions = new ConcurrentHashMap<>();

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Chat server is listening on port " + port);
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();
                Thread.ofVirtual().start(new ClientSession(socket));
            }
        }
    }

    private void broadcast(ChatMessage message) {
        sessions.values().forEach(session -> session.send(message));
    }

    private void sendUserList() {
        String onlineUsers = String.join(", ", sessions.keySet().stream().sorted().toList());
        broadcast(ChatMessage.of(MessageType.USER_LIST, "server", null, onlineUsers));
    }

    private final class ClientSession implements Runnable {
        private final Socket socket;
        private ObjectOutputStream output;
        private String username;

        private ClientSession(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (socket;
                 ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
                output = objectOutput;
                output.flush();

                ChatMessage firstMessage = (ChatMessage) input.readObject();
                if (!register(firstMessage)) {
                    return;
                }

                while (true) {
                    ChatMessage message = (ChatMessage) input.readObject();
                    if (message.type() == MessageType.QUIT) {
                        break;
                    }
                    route(message);
                }
            } catch (EOFException ignored) {
                // The client closed the connection.
            } catch (IOException | ClassNotFoundException exception) {
                System.err.println("Client error: " + exception.getMessage());
            } finally {
                disconnect();
            }
        }

        private boolean register(ChatMessage message) {
            if (message.type() != MessageType.LOGIN || message.sender() == null
                    || message.sender().isBlank()) {
                send(ChatMessage.of(MessageType.LOGIN_RESULT, "server", null, "Invalid login."));
                return false;
            }

            username = message.sender().trim();
            if (sessions.putIfAbsent(username, this) != null) {
                send(ChatMessage.of(
                        MessageType.LOGIN_RESULT,
                        "server",
                        username,
                        "Username is already online."
                ));
                username = null;
                return false;
            }

            send(ChatMessage.of(MessageType.LOGIN_RESULT, "server", username, "Login successful."));
            broadcast(ChatMessage.of(
                    MessageType.SYSTEM,
                    "server",
                    null,
                    username + " joined the chat."
            ));
            sendUserList();
            return true;
        }

        private void route(ChatMessage message) {
            if (message.type() == MessageType.GROUP_CHAT) {
                broadcast(ChatMessage.of(
                        MessageType.GROUP_CHAT,
                        username,
                        null,
                        message.content()
                ));
            } else if (message.type() == MessageType.PRIVATE_CHAT) {
                ClientSession targetSession = sessions.get(message.target());
                if (targetSession == null) {
                    send(ChatMessage.of(
                            MessageType.SYSTEM,
                            "server",
                            username,
                            "Target user is offline."
                    ));
                    return;
                }
                ChatMessage routed = ChatMessage.of(
                        MessageType.PRIVATE_CHAT,
                        username,
                        message.target(),
                        message.content()
                );
                targetSession.send(routed);
                if (targetSession != this) {
                    send(routed);
                }
            } else if (message.type() == MessageType.USER_LIST) {
                sendUserList();
            }
        }

        private synchronized void send(ChatMessage message) {
            if (output == null) {
                return;
            }
            try {
                output.writeObject(message);
                output.flush();
            } catch (IOException exception) {
                System.err.println("Send failed: " + exception.getMessage());
            }
        }

        private void disconnect() {
            if (username == null || !sessions.remove(username, this)) {
                return;
            }
            broadcast(ChatMessage.of(
                    MessageType.SYSTEM,
                    "server",
                    null,
                    username + " left the chat."
            ));
            sendUserList();
        }
    }

    public static void main(String[] args) throws IOException {
        int port = args.length == 0 ? 9090 : Integer.parseInt(args[0]);
        new ChatServer(port).start();
    }
}
