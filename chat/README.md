# GlowChat

JavaFX chat app with:

- a login screen
- server bot chat mode
- peer-to-peer chat mode through the server
- emoji buttons
- bubble-style chat UI

## Run

Compile:

```powershell
javac --module-path "C:\Users\yeabs\Downloads\openjfx-26.0.1_windows-x64_bin-sdk\javafx-sdk-26.0.1\lib" --add-modules javafx.controls,javafx.fxml *.java
```

Start server:

```powershell
java --enable-native-access=javafx.graphics --module-path "C:\Users\yeabs\Downloads\openjfx-26.0.1_windows-x64_bin-sdk\javafx-sdk-26.0.1\lib" --add-modules javafx.controls,javafx.fxml -cp ".;mysql-connector-j-9.7.0.jar" ChatServer
```

Start client:

```powershell
java --enable-native-access=javafx.graphics --module-path "C:\Users\yeabs\Downloads\openjfx-26.0.1_windows-x64_bin-sdk\javafx-sdk-26.0.1\lib" --add-modules javafx.controls,javafx.fxml ChatClient
```
