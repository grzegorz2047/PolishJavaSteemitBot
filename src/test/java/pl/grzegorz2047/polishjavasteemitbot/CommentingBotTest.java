package pl.grzegorz2047.polishjavasteemitbot;

import eu.bittrade.libs.steemj.exceptions.SteemCommunicationException;
import eu.bittrade.libs.steemj.exceptions.SteemResponseException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommentingBotTest {
    @Test
    void checkIfFirstInSpecifiedTag() throws SteemResponseException, SteemCommunicationException {
        CommentingBot commentingBot = new CommentingBot(true, 100, (long) 1000);
        boolean answer = commentingBot.checkIfFirstInSpecifiedTag("polish", "grzegorz2047");
        assertTrue(!answer);
    }

}