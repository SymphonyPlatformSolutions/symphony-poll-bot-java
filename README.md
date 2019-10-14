## Poll Bot
This bot uses Symphony Elements to facilitate the creation of polls, firing of poll messages, ending polls and collation of results. 

![](poll-bot.gif)

## Requirements
* JDK 8+
* Maven 3
* MongoDB
* Symphony Pod 1.55.3 or later
* Symphony API Agent 2.55.9 or later

# Configuration
Fill up `src/main/resources/config.json` with the appropriate values for pod information,
and service account details 
```json5
{
    "sessionAuthHost": "[pod].symphony.com",
    "sessionAuthPort": 443,
    "keyAuthHost": "[pod].symphony.com",
    "keyAuthPort": 443,
    "podHost": "[pod].symphony.com",
    "podPort": 443,
    "agentHost": "[pod].symphony.com",
    "agentPort": 443,
    "botUsername": "poll-bot",
    "botEmailAddress": "poll-bot@bots.symphony.com",
    "botPrivateKeyPath": "rsa/",
    "botPrivateKeyName": "rsa-private-poll-bot.pem"
}
```

Fill up `src/main/resources/application.properties` with the appropriate values for
the uri of your MongoDB instance.
```properties
...
spring.data.mongodb.uri=mongodb+srv://user:pass@mongodb/admin
```
