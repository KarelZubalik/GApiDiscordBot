package org.example.BotEvents;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.example.BotEvents.MessageRules.AllPictures;
import org.example.Google.GooglePhotos;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhotoFromMessage extends ListenerAdapter {
    public PhotoFromMessage() {
        try {
            googlePhotos = new GooglePhotos();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    GooglePhotos googlePhotos;
    AllPictures allPictures;
    Pattern pattern;
    Matcher matcher;

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
                    event.getChannel().sendFiles(allPictures.uniquePicture(event.getGuild().getName())).queue();
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
