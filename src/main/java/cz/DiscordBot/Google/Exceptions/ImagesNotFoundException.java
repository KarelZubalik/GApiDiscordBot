package cz.DiscordBot.Google.Exceptions;

public class ImagesNotFoundException extends Exception{
    public ImagesNotFoundException() {
        super("Pro váš server jíž nejsou žádné unikátní fotky. Kontaktujte prosím admina ať vám nahraje nové.");
    }
}
