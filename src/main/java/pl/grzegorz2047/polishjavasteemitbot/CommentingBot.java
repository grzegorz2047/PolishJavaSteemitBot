package pl.grzegorz2047.polishjavasteemitbot;

import eu.bittrade.libs.steemj.SteemJ;
import eu.bittrade.libs.steemj.apis.database.models.state.Discussion;
import eu.bittrade.libs.steemj.apis.follow.model.BlogEntry;
import eu.bittrade.libs.steemj.base.models.*;
import eu.bittrade.libs.steemj.base.models.operations.CommentOperation;
import eu.bittrade.libs.steemj.enums.DiscussionSortType;
import eu.bittrade.libs.steemj.exceptions.SteemCommunicationException;
import eu.bittrade.libs.steemj.exceptions.SteemInvalidTransactionException;
import eu.bittrade.libs.steemj.exceptions.SteemResponseException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class CommentingBot {

    private final int howDeepToCheckIfFirstPost;
    private final Long frequenceCheckInMilliseconds;
    private String lastAuthorName = "";
    private final boolean debugMode;

    public CommentingBot(boolean debugMode, int howDeepToCheckIfFirstPost, Long frequenceCheckInMilliseconds) {
        this.debugMode = debugMode;
        this.howDeepToCheckIfFirstPost = howDeepToCheckIfFirstPost;
        this.frequenceCheckInMilliseconds = frequenceCheckInMilliseconds;
    }


    public void checkAndMakeWelcomeComments(String tag, String botName, String message, String[] commentTags, AccountName accountWhichCommentsOnPost) throws SteemCommunicationException, SteemResponseException, InterruptedException, SteemInvalidTransactionException {
        // Create a new apiWrapper with your config object.
        SteemJ steemJ = new SteemJ();

        while (true) {
            try {
                doAction(tag, botName, message, commentTags, accountWhichCommentsOnPost, steemJ);
            } catch (Exception ex) {
                Thread.sleep(frequenceCheckInMilliseconds);
                message("Unexpected problem, " + ex.getMessage() + " Trying again", true);
            }
        }
    }

    private void doAction(String tag, String botName, String message, String[] commentTags, AccountName accountWhichCommentsOnPost, SteemJ steemJ) throws Exception {
        List<Discussion> disccusions = getPossibleNewPost(tag, steemJ, frequenceCheckInMilliseconds);
        if (disccusions == null) return;
        Discussion newestDiscussion = disccusions.get(0);//Tu przydaloby sie jakies cachowanie wynikow
        AccountName firstPostAuthor = newestDiscussion.getAuthor();
        String firstPostAuthorName = firstPostAuthor.getName();
        //message("ddd");
        if (isTheSameAuthorAsBefore(firstPostAuthorName)) {
            Thread.sleep(frequenceCheckInMilliseconds);
            return;
        }
        message("Checking if " + firstPostAuthorName + " posted first post in this tag", false);
        boolean isFirst = checkIfFirstInSpecifiedTag(tag, firstPostAuthor.getName());
        //message("dawddawda");
        if (!isFirst) {
            lastAuthorName = firstPostAuthorName;
            Thread.sleep(frequenceCheckInMilliseconds);
            return;
        }
        message("We have user with only one post: " + firstPostAuthorName, false);
        Permlink permlinkToPost = newestDiscussion.getPermlink();
        boolean alreadyCommented = didBotAlreadyCommented(botName, steemJ, firstPostAuthor, permlinkToPost);
        if (!alreadyCommented && !isTheSameAuthorAsBefore(firstPostAuthorName)) {
            message("I comment on " + firstPostAuthorName + "'s post", false);
            try {
                CommentOperation comment = steemJ.createComment(accountWhichCommentsOnPost, firstPostAuthor, permlinkToPost, message, commentTags);
                lastAuthorName = firstPostAuthorName;
                message("Successfuly commented!", false);
            } catch (Exception ex) {
                message("Error while commenting. Possible reasons? Not enough bandwith? API down? ", true);

                Thread.sleep(5000);//delay 5 seconds
            }
        } else {
            if (alreadyCommented) {
                lastAuthorName = firstPostAuthorName;
                message("I already commented on this one for " + firstPostAuthorName, false);
            }
            if (isTheSameAuthorAsBefore(firstPostAuthorName)) {
                message("Already checked before!", false);
            }

        }
        Thread.sleep(frequenceCheckInMilliseconds);
    }

    private void message(String x, boolean isError) {
        if (debugMode) {
            Main.sendMessage(x, isError);
        }
    }

    private List<Discussion> getPossibleNewPost(String tag, SteemJ steemJ, long frequenceCheckInMilliseconds) throws SteemCommunicationException, SteemResponseException, InterruptedException {
        List<Discussion> disccusions = getNewestDiscusions(tag, steemJ);
        if (disccusions.size() == 0) {
            message("Nothing found!", true);
            Thread.sleep(frequenceCheckInMilliseconds);
            return null;
        }
        return disccusions;
    }

    private boolean isTheSameAuthorAsBefore(String firstPostAuthorName) {
        return lastAuthorName.equals(firstPostAuthorName);
    }

    private boolean didBotAlreadyCommented(String botName, SteemJ steemJ, AccountName firstPostAuthor, Permlink permlinkToPost) throws SteemCommunicationException, SteemResponseException {
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
        return alreadyCommented;
    }

    private boolean isAuthorsFirstPost(SteemJ steemJ, AccountName firstPostAuthor) throws SteemCommunicationException, SteemResponseException {
        List<ExtendedAccount> accounts = getAuthorProfileData(steemJ, firstPostAuthor);

        ExtendedAccount extendedAccount = accounts.get(0);
        long commentCount = extendedAccount.getCommentCount();
        long postCount = extendedAccount.getPostCount() - commentCount;
        message("Post count is " + postCount, false);
        return postCount == 1;
    }

    private List<ExtendedAccount> getAuthorProfileData(SteemJ steemJ, AccountName firstPostAuthor) throws SteemCommunicationException, SteemResponseException {
        List<AccountName> list = new ArrayList<>();
        message("Found " + firstPostAuthor.getName() + " author", false);
        list.add(firstPostAuthor);

        return steemJ.getAccounts(list);
    }

    private List<Discussion> getNewestDiscusions(String tag, SteemJ steemJ) throws SteemCommunicationException, SteemResponseException {
        DiscussionQuery discussionQuery = new DiscussionQuery();
        discussionQuery.setTag(tag);
        discussionQuery.setLimit(1);
        return steemJ.getDiscussionsBy(discussionQuery, DiscussionSortType.GET_DISCUSSIONS_BY_CREATED);
    }


    public boolean checkIfFirstInSpecifiedTag(String tag, String newUser) throws Exception {
        SteemJ steemJ = new SteemJ();
        DiscussionQuery newQry = new DiscussionQuery();
        newQry.setLimit(10);
        AccountName author = new AccountName(newUser);
        List<BlogEntry> blogEntries = steemJ.getBlogEntries(author, 0, (short) howDeepToCheckIfFirstPost);
        int numberOfPostsInTag = 0;
        message("===========", false);
        for (BlogEntry blog : blogEntries) {
            Discussion content = steemJ.getContent(author, blog.getPermlink());
            String jsonMetadata = content.getJsonMetadata();
            //message("metadane: " + jsonMetadata);
            List<Object> objects;
            try {
                JSONObject jsonArray = new JSONObject(jsonMetadata);
                JSONArray tags = (JSONArray) jsonArray.get("tags");
                objects = tags.toList();
            } catch (JSONException ex) {
                message("Cannot parse metadata: " + jsonMetadata, true);
                objects = new ArrayList<>();
                throw new Exception("Tactical gotta move out");
            }
            boolean contains = objects.contains(tag);
            //message(tags + " zawiera? " + contains);
            //Permlink parentPermlink = content.getParentPermlink();
            if (contains) {
                message("In " + tag + ": " + content.getTitle(), false);
                numberOfPostsInTag++;
            }
            if (numberOfPostsInTag >= 2) {
                message("===========", false);
                return false;
            }
        }
        message("===========", false);
        return true;
    }
}
