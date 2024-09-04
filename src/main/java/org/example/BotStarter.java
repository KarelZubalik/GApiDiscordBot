package org.example;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.example.BotEvents.PhotoFromMessage;

import java.util.EnumSet;

public class BotStarter {
    public void startBot(){
        EnumSet<GatewayIntent> intents = EnumSet.of(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_MESSAGE_REACTIONS,
                GatewayIntent.DIRECT_MESSAGE_REACTIONS
        );

        try
        {
            DataFromFilesLoader dataFromFilesLoader = new DataFromFilesLoader();
            JDA jda = JDABuilder.createLight(dataFromFilesLoader.getTokenFromFolder(), intents)
                    .addEventListeners(new PhotoFromMessage())
                    .build();
            jda.awaitReady();
            System.out.println("Počet serverů:" + jda.getGuildCache().size());
            System.out.println("Bot naběhl.");
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}
