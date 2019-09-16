package com.symphony.ps.pollbot.model;

import configuration.SymConfig;
import lombok.Data;

@Data
public class PollBotConfig extends SymConfig {
    private String mongoUri;
}
