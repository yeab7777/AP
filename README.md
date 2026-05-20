# Advanced Programming Projects

This repository contains three advanced programming projects developed in Java/JavaFX with database integration and modular design principles:

1. 🎲 **Poker Game** — A card game simulation with betting logic and hand evaluation.
2. 💬 **Chatbot Application** — A multi-user chat system with database persistence and file transfer.
3. 📝 **Notepad App** — A beginner-friendly text editor with font selection, highlights, and bookmarks, connected to a MySQL database via XAMPP.

---

Each project is self-contained with its own source code, resources, and documentation.

---

## 🎲 Poker Game

### Features
- 52-card deck with shuffle and deal logic.
- Texas Hold’em flow: Pre-flop, Flop, Turn, River, Showdown.
- Betting system: fold, check, call, raise.
- Hand evaluation: Royal Flush → High Card.
- Extendable for AI opponents or GUI integration.

### Setup
1. Compile with `javac *.java`.
2. Run with `java PokerGameMain`.
3. Extend with GUI using JavaFX or integrate multiplayer with sockets.

---

## 💬 Chatbot Application

### Features
- Multi-user chat with server-client architecture.
- Database integration (MySQL via XAMPP) for message persistence.
- File transfer between clients.
- Modular design for scalability.

### Setup
1. Start MySQL server in XAMPP.
2. Create database `chatdb` with `messages` table.
3. Compile with `javac *.java`.
4. Run server: `java ChatServer`.
5. Run clients: `java ChatClient`.

---

## 📝 Notepad App

### Features
- Text editing with font selection and text editing.
- Clean, user-friendly JavaFX layout.
- Database integration for saving notes (MySQL via XAMPP).
- Modular folder structure for easy extension.

### Setup
1. Start MySQL server in XAMPP.
2. Create database `notepad` with `notes` table.
3. Compile with `javac *.java`.
4. Run with `java NP`.

---

## ⚙️ Requirements

- Java 17+  
- JavaFX SDK  
- MySQL Connector/J  
- XAMPP (for database hosting)  
- IntelliJ IDEA or Command Prompt for compilation/running  

---

## 🚀 Future Enhancements

- Poker Game: AI opponents, online multiplayer, GUI animations.
- Chatbot: Authentication system, group chats, encryption.
- Notepad: Cloud sync, advanced formatting, export options.

---

## 👩‍💻 Author

Developed by **Yeabsira Tarekegn**  
Software Engineering Student @ Addis Ababa Science and Technology University  

