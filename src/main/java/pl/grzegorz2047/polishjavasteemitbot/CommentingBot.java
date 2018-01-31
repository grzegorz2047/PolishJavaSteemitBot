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
            List<Discussion> disccusions = getPossibleNewPost(tag, steemJ, frequenceCheckInMilliseconds);
            if (disccusions == null) continue;
            Discussion newestDiscussion = disccusions.get(0);//Tu przydaloby sie jakies cachowanie wynikow
            AccountName firstPostAuthor = newestDiscussion.getAuthor();
            String firstPostAuthorName = firstPostAuthor.getName();
            //System.out.println("ddd");
            if (isTheSameAuthorAsBefore(firstPostAuthorName)) {
                Thread.sleep(frequenceCheckInMilliseconds);
                continue;
            }
            System.out.println("Checking if " + firstPostAuthorName + " posted first post in this tag");
            boolean isFirst = checkIfFirstInSpecifiedTag(tag, firstPostAuthor.getName());
            //System.out.println("dawddawda");
            if (!isFirst) {
                lastAuthorName = firstPostAuthorName;
                Thread.sleep(frequenceCheckInMilliseconds);
                continue;
            }
            debugMsg("We have user with only one post: " + firstPostAuthorName);
            Permlink permlinkToPost = newestDiscussion.getPermlink();
            boolean alreadyCommented = didBotAlreadyCommented(botName, steemJ, firstPostAuthor, permlinkToPost);
            if (!alreadyCommented && !isTheSameAuthorAsBefore(firstPostAuthorName)) {
                debugMsg("I comment on " + firstPostAuthorName + "'s post");
                try {
                    CommentOperation comment = steemJ.createComment(accountWhichCommentsOnPost, firstPostAuthor, permlinkToPost, message, commentTags);
                    lastAuthorName = firstPostAuthorName;
                    debugMsg("Successfuly commented!");
                } catch (Exception ex) {
                    debugMsg("Error while commenting. Possible reasons? Not enough bandwith? API down? ");

                }
            } else {
                if (alreadyCommented) {
                    lastAuthorName = firstPostAuthorName;
                    System.out.println("I already commented on this one for " + firstPostAuthorName);
                }
                if (isTheSameAuthorAsBefore(firstPostAuthorName)) {
                    System.out.println("Already checked before!");
                }

            }
            Thread.sleep(frequenceCheckInMilliseconds);
        }
    }

    private void debugMsg(String x) {
        if (debugMode) {
            System.out.println(x);
        }
    }

    private List<Discussion> getPossibleNewPost(String tag, SteemJ steemJ, long frequenceCheckInMilliseconds) throws SteemCommunicationException, SteemResponseException, InterruptedException {
        List<Discussion> disccusions = getNewestDiscusions(tag, steemJ);
        if (disccusions.size() == 0) {
            debugMsg("Nothing found!");
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
        debugMsg("Post count is " + postCount);
        return postCount == 1;
    }

    private List<ExtendedAccount> getAuthorProfileData(SteemJ steemJ, AccountName firstPostAuthor) throws SteemCommunicationException, SteemResponseException {
        List<AccountName> list = new ArrayList<>();
        debugMsg("Found " + firstPostAuthor.getName() + " author");
        list.add(firstPostAuthor);

        return steemJ.getAccounts(list);
    }

    private List<Discussion> getNewestDiscusions(String tag, SteemJ steemJ) throws SteemCommunicationException, SteemResponseException {
        DiscussionQuery discussionQuery = new DiscussionQuery();
        discussionQuery.setTag(tag);
        discussionQuery.setLimit(1);
        return steemJ.getDiscussionsBy(discussionQuery, DiscussionSortType.GET_DISCUSSIONS_BY_CREATED);
    }


    public boolean checkIfFirstInSpecifiedTag(String tag, String newUser) throws SteemCommunicationException, SteemResponseException {
        SteemJ steemJ = new SteemJ();
        DiscussionQuery newQry = new DiscussionQuery();
        newQry.setLimit(10);
        AccountName author = new AccountName(newUser);
        List<BlogEntry> blogEntries = steemJ.getBlogEntries(author, 0, (short) howDeepToCheckIfFirstPost);
        int numberOfPostsInTag = 0;
        for (BlogEntry blog : blogEntries) {
            Discussion content = steemJ.getContent(author, blog.getPermlink());
            Permlink parentPermlink = content.getParentPermlink();
            if (parentPermlink.getLink().equals(tag)) {
                System.out.println("W #polish " + content.getTitle());
                numberOfPostsInTag++;
            }
            if (numberOfPostsInTag >= 2) {
                return false;
            }
        }
        return true;
    }
}
