package cz.DiscordBot.BotEvents;

import cz.DiscordBot.BotEvents.MessageRules.AllPictures;
import cz.DiscordBot.Reactions;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import cz.DiscordBot.Google.Exceptions.ImagesNotFoundException;
import cz.DiscordBot.Google.GooglePhotos;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhotoFromMessage extends ListenerAdapter {
    public PhotoFromMessage(GooglePhotos googlePhotos) {
        this.googlePhotos=googlePhotos;
    }

    GooglePhotos googlePhotos;
    AllPictures allPictures;
    Pattern pattern;
    Matcher matcher;


    //todo Fotky neposílat jako samostatné zprávy, ale posílat jednu zprávu a na tu vytvořit vlákno, kde nahraju fotky.
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        super.onMessageReceived(event);
        if (!event.getAuthor().isBot()) {
            String message = event.getMessage().getContentRaw();
            pattern=Pattern.compile("^send ?([0-9]+)?", Pattern.CASE_INSENSITIVE);
            matcher=pattern.matcher(message);
            if (matcher.matches()) {
                if (allPictures == null) {
                    allPictures = new AllPictures(googlePhotos);
                }
                String pointerStr = matcher.group(1);
                int pointer = Integer.parseInt(pointerStr==null ? "1" : pointerStr);
                // Pokud v příkazu bude požadovaný počet fotek, tak tento počet pošleme
                if (pointer > 5 || pointer < 1) {
                    event.getMessage().addReaction(Emoji.fromUnicode("\uD83D\uDC4E")).queue();
                    event.getChannel().sendMessage("Pokoušíš se poslat více fotek, než je možný rozsah! \n Správný rozsah je 1-5, tedy send 1-5, nebo samostatný 'send'. \n Zadej příkaz správně, pokud chceš vyvolat fotky.").queue();
                    return;
                }
                event.getMessage().addReaction(Emoji.fromUnicode("\uD83D\uDC4D")).queue();

                System.out.println(event.getGuild().getName());
                for (int i = 0; i < pointer; i++) {
                    event.getChannel().sendMessage("Posílám fotku číslo: " + (i + 1) + " z " + pointer + " fotek.").queue();
                    try{
                        event.getChannel().sendFiles(allPictures.uniquePicture(event.getGuild().getName())).queue(sentMessage -> {
                            // Přidání reakcí z pole
                            for (String reaction : Reactions.REACTIONS) {
                                sentMessage.addReaction(Emoji.fromFormatted(reaction)).queue();
                            }
                        });
                   } catch (ImagesNotFoundException e) {
                        event.getChannel().sendMessage(e.getMessage()).queue();
                        break;
                    }
                }

                //Zakomentované posílání fotek z listu fotek. Zanechané pro možnost použít v budoucnu.
//                List<FileUpload> pictures=allPictures.uniquePictures(event.getGuild().getName(),pointer);
//                for (FileUpload picture:pictures){
//                    event.getChannel().sendFiles(picture).queue();
//                }
            }
        }
    }
}
