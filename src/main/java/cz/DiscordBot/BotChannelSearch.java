package cz.DiscordBot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class BotChannelSearch {
    private final JDA jda;

    public BotChannelSearch(JDA jda) {
        this.jda = jda;
    }

    public String getChannelIdByGuildNameAndChannelName(String guildName, String channelName) {
        // Najdi guildu podle názvu
        Guild guild = getGuildByName(guildName);
        if (guild == null) {
            System.out.println("Guild with name " + guildName + " not found.");
            return null;
        }

        // Projdi všechny kanály v guilde a najdi kanál podle názvu
        for (TextChannel channel : guild.getTextChannels()) {
            if (channel.getName().equalsIgnoreCase(channelName)) {
                System.out.println("Found channel with ID: " + channel.getId());
                return channel.getId(); // Vrátí ID kanálu
            }
        }

        System.out.println("Channel with name " + channelName + " not found in guild " + guildName);
        return null;
    }

    // Pomocná metoda pro hledání guildy podle názvu
    private Guild getGuildByName(String name) {
        for (Guild guild : jda.getGuilds()) {
            if (guild.getName().equalsIgnoreCase(name)) {
                return guild; // Vrátí guildu podle názvu
            }
        }
        return null; // Pokud není nalezena
    }
}
