package pl.grzegorz2047.polishjavasteemitbot;

import eu.bittrade.libs.steemj.SteemJ;
import eu.bittrade.libs.steemj.apis.database.models.state.Discussion;
import eu.bittrade.libs.steemj.base.models.*;
import eu.bittrade.libs.steemj.base.models.operations.CommentOperation;
import eu.bittrade.libs.steemj.configuration.SteemJConfig;
import eu.bittrade.libs.steemj.enums.DiscussionSortType;
import eu.bittrade.libs.steemj.enums.PrivateKeyType;
import eu.bittrade.libs.steemj.exceptions.SteemCommunicationException;
import eu.bittrade.libs.steemj.exceptions.SteemInvalidTransactionException;
import eu.bittrade.libs.steemj.exceptions.SteemResponseException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.bitcoinj.core.AddressFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);


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
            Properties data = properties.getProperties();
            String watchedTag = data.getProperty("watchedTag");
            String botName = data.getProperty("botName");
            String postingKey = data.getProperty("postingKey");
            String content = data.getProperty("message");
            String commentTagsString = data.getProperty("commentTags");

            SteemJConfig steemConfig = createSteemConfig(botName, postingKey);
            CommentingBot commentingBot = new CommentingBot();
            String[] listOfCommentTags = commentTagsString.split(",");
            AccountName defaultAccount = steemConfig.getDefaultAccount();

            commentingBot.checkAndMakeWelcomeComments(watchedTag, botName, content, listOfCommentTags, defaultAccount);
        } catch (SteemResponseException e) {
            LOGGER.error("An error occured.", e);
            LOGGER.error("The error code is {}", e.getCode());
        } catch (SteemCommunicationException e) {
            LOGGER.error("A communication error occured!", e);
        } catch (InterruptedException | IOException | SteemInvalidTransactionException e) {
            e.printStackTrace();
        } catch (AddressFormatException e) {
            System.out.println("Klucz prywatny jest nieprawidlowy!");
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


/*
    private static void fun() throws SteemCommunicationException, SteemResponseException {
        SteemJ steemJ = new SteemJ();

        DiscussionQuery newQry = new DiscussionQuery();
        newQry.setTag("polish");
        newQry.setLimit(10);
        newQry.setStartAuthor( new AccountName("grzegorz2047"));
        newQry.setSelectAuthors(Collections.singletonList(newQry.getStartAuthor()));
        List<ExtendedAccount> grzex = steemJ.getAccounts(Collections.singletonList(newQry.getStartAuthor()));
        ExtendedAccount extendedAccountgr = grzex.get(0);

        List<Discussion> discussionsInPolishByGrzegorz2047 = steemJ.getDiscussionsBy(newQry, DiscussionSortType.GET_DISCUSSIONS_BY_CREATED);

//            discussionsInPolishByGrzegorz2047.size();
        System.out.println("Number of discussions in polish " + discussionsInPolishByGrzegorz2047.size());
        for (Discussion d : discussionsInPolishByGrzegorz2047) {
            System.out.println("Dyskusja " + d.getTitle());
        }
    }*/


}
