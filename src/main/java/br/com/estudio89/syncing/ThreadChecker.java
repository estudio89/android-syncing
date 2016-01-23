package br.com.estudio89.syncing;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by luccascorrea on 11/28/14.
 *
 * This keeps track of which threads are allowed to execute.
 */
public class ThreadChecker {
    private List<String> threadIds = new ArrayList<>();

    /**
     * This method generates a new identifier (the current date in milliseconds)
     * and adds it to the threadIds list, which keeps the ids of all threads
     * in execution.
     *
     * @return new thread id
     *
     */
    public String setNewThreadId() {
        String threadId = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
        threadIds.add(threadId);
        return threadId;
    }

    /**
     * Verifies if the given thread id is in the list of identifiers.
     *
     * @param threadId the thread identifier
     * @return true if valid, false otherwise
     */
    public boolean isValidThreadId(String threadId) {
        return this.threadIds.contains(threadId);
    }

    /**
     * Removes a thread id from the list. This is called after sync operations.
     *
     * @param threadId the thread identifier
     */
    public void removeThreadId(String threadId) {
        this.threadIds.remove(threadId);
    }

    /**
     * Clears all thread ids.
     */
    public void clear(){ this.threadIds.clear(); }
}
