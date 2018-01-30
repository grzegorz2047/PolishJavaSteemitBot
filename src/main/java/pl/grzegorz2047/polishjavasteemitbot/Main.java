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
            Path pathToFile = Paths.get("bot.properties");
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
            String commentTags = data.getProperty("commentTags");

            SteemJConfig steemConfig = createSteemConfig(botName, postingKey);

            checkAndMakeWelcomeComments(steemConfig, watchedTag, botName, content, commentTags.split(","));
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
        SteemJConfig myConfig = SteemJConfig.getInstance();
        myConfig.setResponseTimeout(100000);
        myConfig.setDefaultAccount(new AccountName(botName));

        List<ImmutablePair<PrivateKeyType, String>> privateKeys = new ArrayList<>();
        privateKeys.add(new ImmutablePair<>(PrivateKeyType.POSTING, postingKey));

        myConfig.getPrivateKeyStorage().addAccount(myConfig.getDefaultAccount(), privateKeys);
        return myConfig;
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

    private static void checkAndMakeWelcomeComments(SteemJConfig myConfig, String tag, String botName, String message, String[] commentTags) throws SteemCommunicationException, SteemResponseException, InterruptedException, SteemInvalidTransactionException {
        // Create a new apiWrapper with your config object.
        SteemJ steemJ = new SteemJ();

        while (true) {
            DiscussionQuery discussionQuery = new DiscussionQuery();
            discussionQuery.setTag(tag);
            discussionQuery.setLimit(1);
            List<Discussion> disccusions = steemJ.getDiscussionsBy(discussionQuery, DiscussionSortType.GET_DISCUSSIONS_BY_CREATED);
            if (disccusions.size() == 0) {
                System.out.println("Nothing found!");
                Thread.sleep(1000);
                continue;
            }
            Discussion discussion = disccusions.get(0);
            AccountName firstPostAuthor = discussion.getAuthor();
            List<AccountName> list = new ArrayList<>();
            System.out.println("Found " + firstPostAuthor.getName() + " author");
            list.add(firstPostAuthor);

            List<ExtendedAccount> accounts = steemJ.getAccounts(list);

            ExtendedAccount extendedAccount = accounts.get(0);
            long commentCount = extendedAccount.getCommentCount();
            long postCount = extendedAccount.getPostCount() - commentCount;
            System.out.println("Post count is " + postCount);
            boolean isFirst = postCount == 1;

            if (isFirst) {
                System.out.println("We have user with only one post: " + firstPostAuthor.getName());
                Permlink permlinkToPost = discussion.getPermlink();
                List<Discussion> contentReplies = steemJ.getContentReplies(firstPostAuthor, permlinkToPost);
                boolean alreadyCommented = false;
                for (Discussion commentPost : contentReplies) {
                    AccountName commentPostAuthor = commentPost.getAuthor();
                    String commentPostAuthorName = commentPostAuthor.getName();
                    boolean botAnswered = commentPostAuthorName.equals(botName);
                    if (botAnswered) {
                        alreadyCommented = true;
                        break;
                    }
                }
                if (!alreadyCommented) {
                    System.out.println("I'm comment on " + firstPostAuthor.getName() + "'s post");
                    AccountName accountWhichCommentsOnPost = myConfig.getDefaultAccount();
                    CommentOperation comment = steemJ.createComment(accountWhichCommentsOnPost, firstPostAuthor, permlinkToPost, message, commentTags);
                }
            }
            Thread.sleep(1000);
        }
    }
}
