package cz.DiscordBot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import cz.DiscordBot.BotEvents.MessageRules.AllPictures;
import cz.DiscordBot.Google.Exceptions.ImagesNotFoundException;
import cz.DiscordBot.Google.GooglePhotos;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static cz.DiscordBot.Reactions.REACTIONS;

public class ScheduledMessageBot {


    private final JDA jda;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private GooglePhotos googlePhotos;

    public ScheduledMessageBot(JDA jda,GooglePhotos googlePhotos) {
        this.googlePhotos=googlePhotos;
        this.jda = jda;
        planFromConfigFolder();
    }

    public void planFromConfigFolder() {
        List<String[]> configRows = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File(DataFromFilesLoader.DAILY_SEND_CONFIG()))) {
            while (scanner.hasNext()) {
                configRows.add(scanner.nextLine().split(";"));
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        BotChannelSearch botChannelSearch = new BotChannelSearch(jda);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        for (int e = 0; e < configRows.size(); e++) {
            String[] configRow = configRows.get(e);
            String[] dates = configRow[2].split(",");
            List<LocalTime> localTimeList = new ArrayList<>();
            for (String date : dates) {
                localTimeList.add(LocalTime.parse(date, formatter));
            }
            startDailyScheduledMessages(botChannelSearch.getChannelIdByGuildNameAndChannelName(configRow[0], configRow[1]), localTimeList);
        }
    }

    // Funkce pro naplánování zpráv na specifické časy v daný den
    public void startDailyScheduledMessages(String channelId, List<LocalTime> scheduledTimes) {
        // Získání kanálu podle ID
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            System.out.println("Channel not found!");
            return;
        }



        // Funkce pro naplánování zpráv
        Runnable task = () -> {
            LocalDate today = LocalDate.now();
            ZonedDateTime now = ZonedDateTime.now();
            ZoneId zoneId = now.getZone(); // Použijeme aktuální časovou zónu systému

            // Pro každý čas naplánuj zprávu
            for (LocalTime localTime : scheduledTimes) {
                // Převod LocalTime na ZonedDateTime pro dnešní datum
                ZonedDateTime scheduledDateTime = localTime.atDate(today).atZone(zoneId);
                long initialDelay = Duration.between(now, scheduledDateTime).toMillis();

                // Pokud je naplánovaný čas již minulý, přidej 1 den
                if (initialDelay <= 0) {
                    scheduledDateTime = scheduledDateTime.plusDays(1);
                    initialDelay = Duration.between(now, scheduledDateTime).toMillis();
                }
                AllPictures allPictures = new AllPictures(googlePhotos);
                // Naplánování zprávy
                scheduler.schedule(() -> {
                    channel.sendMessage("Automatic sender ;)").queue();
                    for (int i = 0; i < 5; i++) {
                        try {
                            channel.sendFiles(allPictures.uniquePicture(channel.getGuild().getName())).queue(sentMessage -> {
                                // Přidání reakcí z pole
                                for (String reaction : REACTIONS) {
                                    sentMessage.addReaction(Emoji.fromFormatted(reaction)).queue();
                                }
                            });
                        } catch (ImagesNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, initialDelay, TimeUnit.MILLISECONDS);

                System.out.println("Message scheduled for: " + scheduledDateTime);
            }
        };

        // Spuštění úkolu pro první naplánování zpráv
        task.run();

        // Po uplynutí 24 hodin naplánujeme opakování tohoto úkolu na další den
        scheduler.scheduleAtFixedRate(task, 1, 1, TimeUnit.DAYS);
    }

    public void stopScheduler() {
        scheduler.shutdown();
    }
}