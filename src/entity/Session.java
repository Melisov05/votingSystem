package entity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Session {
    private static final Map<String, Candidate> userSessions = new ConcurrentHashMap<>();

    public static String createSession(Candidate candidate) {
        String sessionId = UUID.randomUUID().toString();
        userSessions.put(sessionId, candidate);
        return sessionId;
    }

    public static Candidate getCandidateForSession(String sessionId) {
        return userSessions.get(sessionId);
    }
}
