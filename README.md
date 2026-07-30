# Java Socket Chat

A lightweight Java network-programming course project built with a client/server architecture.

## Features

- Username login with duplicate-online validation
- Group chat and private messaging
- Real-time online user list
- Join, leave and offline-target notifications
- Graceful handling of disconnected clients

## Design

- `ServerSocket` and `Socket` for TCP communication
- Serializable record-based message protocol
- One virtual thread per client connection
- `ConcurrentHashMap` for thread-safe online session management
- Console client commands for a small and testable interaction surface

## Requirements

- JDK 21 or later
- Maven 3.9+ (optional)

## Build and run

```bash
mvn clean package
java -cp target/classes com.zhaoxuchun.chat.ChatServer
```

Start one or more clients in another terminal:

```bash
java -cp target/classes com.zhaoxuchun.chat.ChatClient
```

Client commands:

```text
/all hello everyone
/to alice hello
/users
/quit
```
