package org.example.BotEvents.MessageRules;

import net.dv8tion.jda.api.utils.FileUpload;
import org.example.Google.GooglePhotos;

import java.io.IOException;
import java.util.List;

public class AllPictures {
    public AllPictures(GooglePhotos googlePhotos) {
        this.googlePhotos = googlePhotos;
    }

    GooglePhotos googlePhotos;
    public List<FileUpload> uniquePictures(String server,int quantity){
            List<FileUpload> files;
            try {
                files = googlePhotos.getListOfPictures(server,quantity);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return files;
    }
    public FileUpload uniquePicture(String server){
        return googlePhotos.getUniquePicture(server);
    }
}
