package client;

import java.io.*;
import java.net.*;

public class Client {
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    public Client(String host, int port) throws IOException {
        socket = new Socket(host, port);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    public void send(String message) throws IOException {
        out.write(message);
        out.newLine();
        out.flush();
    }

    public BufferedReader getReader() {
        return in;
    }

    public void close() throws IOException {
        socket.close();
    }
}
