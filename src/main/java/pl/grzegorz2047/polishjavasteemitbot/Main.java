package pl.grzegorz2047.polishjavasteemitbot;

import eu.bittrade.libs.steemj.base.models.AccountName;
import eu.bittrade.libs.steemj.configuration.SteemJConfig;
import eu.bittrade.libs.steemj.enums.PrivateKeyType;
import eu.bittrade.libs.steemj.exceptions.SteemCommunicationException;
import eu.bittrade.libs.steemj.exceptions.SteemInvalidTransactionException;
import eu.bittrade.libs.steemj.exceptions.SteemResponseException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bitcoinj.core.AddressFormatException;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.logging.*;

import static java.util.logging.Logger.getLogger;

public class Main {
    private static final Logger LOGGER = getLogger("PolishBot");
    private static final org.slf4j.Logger slogger = LoggerFactory.getLogger("PolishBot");

    //https://github.com/marvin-we/steem-java-api-wrapper/tree/0.4.x/sample/src/main/java/my/sample/project
    public static void main(String args[]) {
        try {
            PropertiesHandler properties = new PropertiesHandler();
            String propertiesFileName = "bot.properties";
            Path pathToFile = Paths.get(propertiesFileName);
            if (!fileExists(pathToFile)) {
                System.out.println("File bot.properties generated! Enter bot informations to file before you run a bot!");
                properties.createFileProperties();
                System.exit(0);
            }
            setupFileLogging();
            runCommentingBot(properties);
        } catch (SteemResponseException e) {
            sendMessage("An error occured.", e, true);
            sendMessage("The error code is " + e.getCode(), true);
        } catch (SteemCommunicationException e) {
            sendMessage("A communication error occured!", e, true);
        } catch (InterruptedException | IOException | SteemInvalidTransactionException e) {
            e.printStackTrace();
        } catch (AddressFormatException e) {
            sendMessage("Klucz prywatny jest nieprawidlowy!", true);
        }
    }

    private static void runCommentingBot(PropertiesHandler properties) throws IOException, SteemCommunicationException, SteemResponseException, InterruptedException, SteemInvalidTransactionException {
        Properties data = properties.getProperties();
        String watchedTag = data.getProperty("watchedTag");
        String botName = data.getProperty("botName");
        String postingKey = data.getProperty("postingKey");
        String content = data.getProperty("message");
        String commentTagsString = data.getProperty("commentTags");
        boolean debugMode = Boolean.parseBoolean(data.getProperty("debug"));
        boolean votingEnabled = Boolean.parseBoolean(data.getProperty("votingEnabled"));
        int votingPower = Integer.valueOf(data.getProperty("votingPower"));
        Long frequenceCheckInMilliseconds = Long.valueOf(data.getProperty("frequenceCheckInMilliseconds"));
        int howDeepToCheckIfFirstPost = Integer.valueOf(data.getProperty("howDeepToCheckIfFirstPost"));
        boolean reblogEnabled = Boolean.parseBoolean(data.getProperty("reblogEnabled"));
        boolean intervalEnabled = Boolean.parseBoolean(data.getProperty("intervalsEnabled"));
        int votingPowerLimit = Integer.parseInt(data.getProperty("votingPowerLimit"));
        String intervals = data.getProperty("intervals");

        List<Interval> intervalsList = new ArrayList<>();
        if (intervalEnabled) {
            IntervalParse intervalParse = new IntervalParse();
            intervalsList = intervalParse.parse(intervals);

        }

        if (intervalsList.isEmpty()) {
            intervalsList.add(new Interval(0, 100, votingPower));
        }


        SteemJConfig steemConfig = createSteemConfig(botName, postingKey);
        CommentingBot commentingBot = new CommentingBot(debugMode, howDeepToCheckIfFirstPost, frequenceCheckInMilliseconds, votingEnabled, reblogEnabled, votingPowerLimit, intervalsList);
        String[] listOfCommentTags = commentTagsString.split(",");
        AccountName defaultAccount = steemConfig.getDefaultAccount();
        LOGGER.log(Level.INFO, "Bot is started!");

        commentingBot.checkAndMakeWelcomeComments(watchedTag, botName, content, listOfCommentTags, defaultAccount);
    }

    public static void sendMessage(String msg, boolean isError) {
        if (isError) {
            LOGGER.log(Level.SEVERE, msg);
        } else {
            LOGGER.log(Level.INFO, msg);
        }
    }

    public static void sendMessage(String msg, Throwable thrown, boolean isError) {
        if (isError) {
            LOGGER.log(Level.SEVERE, msg, thrown);
        } else {
            LOGGER.log(Level.INFO, msg, thrown);
        }
    }

    private static void setupFileLogging() {
        FileHandler fh;

        try {
            for (Handler handler : LOGGER.getHandlers()) {
                LOGGER.removeHandler(handler);
            }
            // This block configure the logger with handler and formatter
            fh = new FileHandler("bot.log");
            LOGGER.addHandler(fh);
            fh.setFormatter(new SimpleFormatter());
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static SteemJConfig createSteemConfig(String botName, String postingKey) {
        SteemJConfig steemJConfig = SteemJConfig.getInstance();
        steemJConfig.setResponseTimeout(100000);
        AccountName botAccount = new AccountName(botName);
        steemJConfig.setDefaultAccount(botAccount);

        steemJConfig.setSteemJWeight((short) 0);//https://github.com/marvin-we/steem-java-api-wrapper/blob/126c907c4d136d38d4e805153aae1457f0a8f5e6/core/src/main/java/eu/bittrade/libs/steemj/SteemJ.java#L3018 ????
        List<ImmutablePair<PrivateKeyType, String>> privateKeys = new ArrayList<>();
        privateKeys.add(new ImmutablePair<>(PrivateKeyType.POSTING, postingKey));

        steemJConfig.getPrivateKeyStorage().addAccount(botAccount, privateKeys);
        return steemJConfig;
    }

    private static boolean fileExists(Path pathToFile) {
        return Files.exists(pathToFile);
    }

}
