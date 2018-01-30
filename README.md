# PolishJavaSteemitBot

Hey, if you want to use this bot do the following steps:

1. Get PC
2. Get JAVA on that PC
3. Get private posting key for account which will be used as bot
4. Get name of this account

Once you get those things above, run the bot for the first time.

File, bot.properties will be generated.

In this file you can specify how often bot looks for new user. Also you can specify how "deep" he should look to check whether post is first in specified tag.

Informations which you can set in bot.properties:

commentTags=welcome,first,post,cool
watchedTag=introducemyself
message=Here comment text
botName=botAccountName
postingKey=yourprivatepostingkey
frequenceCheckInMilliseconds=1000
debug=true
HowDeepToCheckIfFirstPost=100

To run bot use command in batch file or console: java -jar PolishJavaSteemBot-1.0-SNAPSHOT.jar

You can also specify various java params, but it's not required

To kill bot, simply use ctrl + c

My steemit account is

https://steemit.com/@grzegorz2047
