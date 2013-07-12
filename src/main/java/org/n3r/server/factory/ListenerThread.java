package org.n3r.server.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListenerThread implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(LinuxLogContentCollector.class);
    private Thread outerThread;
    private Process process;

    public ListenerThread(Thread outerThread) {
        this.outerThread = outerThread;
    }

    public ListenerThread(Thread outerThread, Process process) {
        this.outerThread = outerThread;
        this.process = process;
    }

    @Override
    public void run() {
        while(!outerThread.isInterrupted()) {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
            }
        }
        process.destroy();
        log.info("outer thread is over! ");
    }

}
