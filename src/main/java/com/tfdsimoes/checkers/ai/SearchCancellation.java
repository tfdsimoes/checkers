package com.tfdsimoes.checkers.ai;

import java.util.concurrent.CancellationException;

final class SearchCancellation {
    private SearchCancellation() {
    }

    static void checkInterrupted() {
        if (Thread.currentThread().isInterrupted()) {
            throw new CancellationException("AI search was interrupted");
        }
    }
}
