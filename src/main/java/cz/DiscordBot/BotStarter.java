package cz.DiscordBot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import cz.DiscordBot.BotEvents.PhotoFromMessage;
import cz.DiscordBot.Google.GooglePhotos;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.EnumSet;

public class BotStarter {
    private GooglePhotos googlePhotos;

    public void startBot() {
        EnumSet<GatewayIntent> intents = EnumSet.of(
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_MESSAGE_REACTIONS,
                GatewayIntent.DIRECT_MESSAGE_REACTIONS
        );

        try {
            googlePhotos = new GooglePhotos();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
        try {
            DataFromFilesLoader dataFromFilesLoader = new DataFromFilesLoader();
            JDA jda = JDABuilder.createLight(dataFromFilesLoader.getTokenFromFolder(), intents)
                    .addEventListeners(new PhotoFromMessage(googlePhotos))
                    .build();
            jda.awaitReady();
            System.out.println("Počet serverů:" + jda.getGuildCache().size());
            System.out.println("Bot naběhl.");
            ScheduledMessageBot scheduledMessageBot = new ScheduledMessageBot(jda,googlePhotos);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
