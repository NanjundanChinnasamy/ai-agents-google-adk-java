package com.google.adk.socialspark.agui;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SessionStore {

    public static class ThreadSession {
        private final String threadId;
        private final ConcurrentHashMap<String, Object> state = new ConcurrentHashMap<>();
        private final CopyOnWriteArrayList<AgUiModels.AgUiMessage> messages = new CopyOnWriteArrayList<>();

        public ThreadSession(String threadId) {
            this.threadId = threadId;
        }

        public String getThreadId() { return threadId; }
        public ConcurrentHashMap<String, Object> getState() { return state; }
        public CopyOnWriteArrayList<AgUiModels.AgUiMessage> getMessages() { return messages; }
    }

    private static final ConcurrentHashMap<String, ThreadSession> sessions = new ConcurrentHashMap<>();

    public static ThreadSession getOrCreate(String threadId) {
        return sessions.computeIfAbsent(threadId, ThreadSession::new);
    }

    public static ThreadSession get(String threadId) {
        return sessions.get(threadId);
    }

    private SessionStore() {}
}
