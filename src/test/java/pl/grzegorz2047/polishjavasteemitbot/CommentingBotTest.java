package pl.grzegorz2047.polishjavasteemitbot;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentingBotTest {
    @Test
    void checkIfFirstInSpecifiedTag() throws Exception {
        CommentingBot commentingBot = new CommentingBot(true, 100, (long) 1000, false, false, 90, Collections.singletonList(new Interval(30, 90, 100)));
        boolean answer = commentingBot.checkIfFirstInSpecifiedTag("polish", "grzegorz2047");
        assertTrue(!answer);
    }

}