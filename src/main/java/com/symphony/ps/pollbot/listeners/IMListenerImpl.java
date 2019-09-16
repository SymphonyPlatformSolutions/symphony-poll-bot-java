package com.symphony.ps.pollbot.listeners;

import clients.SymBotClient;
import com.mongodb.client.MongoCollection;
import com.symphony.ps.pollbot.PollBot;
import com.symphony.ps.pollbot.model.Poll;
import com.symphony.ps.pollbot.model.PollAnswer;
import com.symphony.ps.pollbot.model.PollParticipant;
import java.time.Instant;
import java.util.Arrays;
import listeners.IMListener;
import model.*;
import utils.FormBuilder;

public class IMListenerImpl implements IMListener {
    private SymBotClient botClient;

    public IMListenerImpl() {
        this.botClient = PollBot.getBotClient();
    }

    public void onIMMessage(InboundMessage msg) {
        MongoCollection<Poll> pollCollection = PollBot.getDataProvider().getPollCollection();

        pollCollection.insertOne(
            Poll.builder()
                .pollId(123)
                .creator(345)
                .created(Instant.now())
                .questionText("Why is the world round?")
                .type(StreamTypes.IM)
                .participants(Arrays.asList(
                    new PollParticipant(999, "xyz"),
                    new PollParticipant(888, "abc")
                ))
                .answers(Arrays.asList(
                    new PollAnswer(2345, "Hello"),
                    new PollAnswer(2345, "Bye Bye")
                ))
                .build()
        );

        String messageText;
        if (msg.getMessageText().equalsIgnoreCase("/elements")) {
            messageText = FormBuilder.builder("form_id")
                .addHeader(2, "Form-Title")
                .addHeader(4, "Name")
                .addTextField("name_01", "", "Enter your name", true, false, 5, 100)
                .addHeader(4, "Select your country")
                .addDropdownMenu("country", false, Arrays.asList(
                    new DropdownMenuOption("Australia", "Australia", true),
                    new DropdownMenuOption("Brazil", "Brazil", false),
                    new DropdownMenuOption("China", "China", false),
                    new DropdownMenuOption("Denmark", "Denmark", false),
                    new DropdownMenuOption("Ecuador", "Ecuador", false),
                    new DropdownMenuOption("France", "France", false),
                    new DropdownMenuOption("Germany", "Germany", false),
                    new DropdownMenuOption("Italy", "Italy", false),
                    new DropdownMenuOption("Japan", "Japan", false)
                ))
                .addHeader(4, "Choose your radio option")
                .addRadioButton("example_radio", "Marked", "option_01", true)
                .addRadioButton("example_radio", "Unmarked", "option_02", false)
                .addHeader(4, "Choose your checkbox option")
                .addCheckBox("checkbox_1", "Checked", "value01", true)
                .addCheckBox("checkbox_2", "Unchecked", "value02", false)
                .addHeader(4, "Send a comment")
                .addTextArea("comment", "", "In my opinion...", true)
                .addButton("reset", "Reset", FormButtonType.RESET)
                .addButton("submit_button", "Submit", FormButtonType.ACTION)
                .formatElement();
        } else {
            messageText = "<h3>Type '/elements' to render a form</h3>";
        }

        OutboundMessage msgOut = new OutboundMessage(messageText);
        botClient.getMessagesClient().sendMessage(msg.getStream().getStreamId(), msgOut);
    }

    public void onIMCreated(Stream stream) {}
}
