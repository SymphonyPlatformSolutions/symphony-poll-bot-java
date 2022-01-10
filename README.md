## Poll Bot
This bot uses Symphony Elements to facilitate the creation of polls, firing of poll messages, ending polls and collation of results. 

![](poll-bot.gif)

## Requirements
* JDK 17+
* MongoDB

# Configuration
Fill up `src/main/resources/application.yaml` with the values for pod information and a mongo database uri
```yaml
spring:
  data.mongodb.uri: mongodb://

bdk:
  host: develop2.symphony.com
  bot:
    username: poll-bot-java
    privateKey.path: rsa/privatekey.pem
```
