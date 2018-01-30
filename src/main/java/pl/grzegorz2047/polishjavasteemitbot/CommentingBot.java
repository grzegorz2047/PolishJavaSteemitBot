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
                if (!alreadyCommented && !lastAuthorName.equals(firstPostAuthor.getName())) {
                    System.out.println("I'm comment on " + firstPostAuthor.getName() + "'s post");
                    try {
                        CommentOperation comment = steemJ.createComment(accountWhichCommentsOnPost, firstPostAuthor, permlinkToPost, message, commentTags);
                        lastAuthorName = firstPostAuthor.getName();
                    } catch (Exception ex) {
                        System.out.println("Error while commenting " + ex.getCause().toString());

                    }
                }
            }
            Thread.sleep(1000);
        }
    }
}
