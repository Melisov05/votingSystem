import server.VotingServer;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            VotingServer server = new VotingServer("localhost", 9889);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}