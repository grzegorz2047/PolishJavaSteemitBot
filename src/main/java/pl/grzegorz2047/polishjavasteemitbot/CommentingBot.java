package pl.grzegorz2047.polishjavasteemitbot;

import eu.bittrade.libs.steemj.SteemJ;
import eu.bittrade.libs.steemj.apis.database.models.state.Discussion;
import eu.bittrade.libs.steemj.base.models.AccountName;
import eu.bittrade.libs.steemj.base.models.DiscussionQuery;
import eu.bittrade.libs.steemj.base.models.ExtendedAccount;
import eu.bittrade.libs.steemj.base.models.Permlink;
import eu.bittrade.libs.steemj.base.models.operations.CommentOperation;
import eu.bittrade.libs.steemj.enums.DiscussionSortType;
import eu.bittrade.libs.steemj.exceptions.SteemCommunicationException;
import eu.bittrade.libs.steemj.exceptions.SteemInvalidTransactionException;
import eu.bittrade.libs.steemj.exceptions.SteemResponseException;

import java.util.ArrayList;
import java.util.List;

public class CommentingBot {

    private static String lastAuthorName = "";


    public void checkAndMakeWelcomeComments(String tag, String botName, String message, String[] commentTags, AccountName accountWhichCommentsOnPost) throws SteemCommunicationException, SteemResponseException, InterruptedException, SteemInvalidTransactionException {
        // Create a new apiWrapper with your config object.
        SteemJ steemJ = new SteemJ();

        while (true) {
            List<Discussion> disccusions = getPossibleNewPost(tag, steemJ);
            if (disccusions == null) continue;
            Discussion newestDiscussion = disccusions.get(0);
            AccountName firstPostAuthor = newestDiscussion.getAuthor();
            boolean isFirst = isAuthorsFirstPost(steemJ, firstPostAuthor);

            if (isFirst) {
                String firstPostAuthorName = firstPostAuthor.getName();
                System.out.println("We have user with only one post: " + firstPostAuthorName);
                Permlink permlinkToPost = newestDiscussion.getPermlink();
                boolean alreadyCommented = didBotAlreadyCommented(botName, steemJ, firstPostAuthor, permlinkToPost);
                if (!alreadyCommented && !isTheSameAuthorAsBefore(firstPostAuthorName)) {
                    System.out.println("I'm comment on " + firstPostAuthorName + "'s post");
                    try {
                        CommentOperation comment = steemJ.createComment(accountWhichCommentsOnPost, firstPostAuthor, permlinkToPost, message, commentTags);
                        lastAuthorName = firstPostAuthorName;
                    } catch (Exception ex) {
                        System.out.println("Error while commenting " + ex.getCause().toString());

                    }
                }
            }
            Thread.sleep(1000);
        }
    }

    private List<Discussion> getPossibleNewPost(String tag, SteemJ steemJ) throws SteemCommunicationException, SteemResponseException, InterruptedException {
        List<Discussion> disccusions = getNewestDiscusions(tag, steemJ);
        if (disccusions.size() == 0) {
            System.out.println("Nothing found!");
            Thread.sleep(1000);
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
        System.out.println("Post count is " + postCount);
        return postCount == 1;
    }

    private List<ExtendedAccount> getAuthorProfileData(SteemJ steemJ, AccountName firstPostAuthor) throws SteemCommunicationException, SteemResponseException {
        List<AccountName> list = new ArrayList<>();
        System.out.println("Found " + firstPostAuthor.getName() + " author");
        list.add(firstPostAuthor);

        return steemJ.getAccounts(list);
    }

    private List<Discussion> getNewestDiscusions(String tag, SteemJ steemJ) throws SteemCommunicationException, SteemResponseException {
        DiscussionQuery discussionQuery = new DiscussionQuery();
        discussionQuery.setTag(tag);
        discussionQuery.setLimit(1);
        return steemJ.getDiscussionsBy(discussionQuery, DiscussionSortType.GET_DISCUSSIONS_BY_CREATED);
    }
}