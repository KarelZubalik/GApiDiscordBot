package cz.DiscordBot.Google;


import autovalue.shaded.com.google.common.collect.ImmutableList;
import com.google.api.gax.rpc.ApiException;
import com.google.photos.library.v1.PhotosLibraryClient;
import com.google.photos.library.v1.internal.InternalPhotosLibraryClient;
import com.google.photos.library.v1.proto.*;
import com.google.photos.library.v1.upload.UploadMediaItemRequest;
import com.google.photos.library.v1.upload.UploadMediaItemResponse;
import com.google.photos.library.v1.util.NewMediaItemFactory;
import com.google.photos.types.proto.MediaItem;
import com.google.rpc.Code;
import com.google.rpc.Status;
import cz.DiscordBot.DataFromFilesLoader;
import cz.DiscordBot.Google.Exceptions.ImagesNotFoundException;
import cz.DiscordBot.Google.Factories.PhotosLibraryClientFactory;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class GooglePhotos {
    List<MediaItem> items;

    private static final List<String> REQUIRED_SCOPES =
            ImmutableList.of("https://www.googleapis.com/auth/photoslibrary.readonly", "https://www.googleapis.com/auth/photoslibrary.appendonly", "https://www.googleapis.com/auth/photoslibrary.edit.appcreateddata");
    private PhotosLibraryClient photosLibraryClient;

    public GooglePhotos() throws GeneralSecurityException, IOException {
        try {
            System.out.println(DataFromFilesLoader.GOOGLE_API_FOLDER());
            photosLibraryClient = PhotosLibraryClientFactory.createClient(DataFromFilesLoader.GOOGLE_API_FOLDER(), REQUIRED_SCOPES);
            items = new ArrayList<>();
            inizializeItemsIfNeeded();
        } catch (Exception e) {
//            System.out.println(e.getMessage()+"\n"+e.getLocalizedMessage()+"\n"+e.getCause().getMessage());
            if (e.getCause().getMessage().contains("UNAVAILABLE: Credentials failed to obtain metadata")){
                System.out.println("Špatný login, proveďte manuální login.");
                try {
                    //smazání aktuálního souboru s google tokenem pro nové vytvoření.
                    Path path= Paths.get(DataFromFilesLoader.GOOGLE_REFRESH_TOKEN_FOLDER());
                    Files.delete(path);
                    photosLibraryClient = PhotosLibraryClientFactory.createClient(DataFromFilesLoader.GOOGLE_API_FOLDER(), REQUIRED_SCOPES);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                inizializeItemsIfNeeded();
            }else {
                throw new RuntimeException(e);
            }
        }
    }

    private void inizializeItemsIfNeeded() {
        if (items.isEmpty()) {
            InternalPhotosLibraryClient.ListMediaItemsPagedResponse response = photosLibraryClient.listMediaItems(ListMediaItemsRequest.newBuilder().setPageSize(100).build());
            System.out.println("itemy při iterateAll");
            for (MediaItem item : response.iterateAll()) {
                System.out.println(item.getId());
                items.add(item);
            }
        }
    }

    private List<MediaItem> getFilteredItems(String server) {
        List<MediaItem> mediaItemListToReturn=new ArrayList<>(items.stream().filter(mediaItem -> !mediaItem.getDescription().contains(server)
        ).toList());
        if (mediaItemListToReturn.size()<100){
            System.out.println("U serveru:"+server+" zbývá jíž jen:"+mediaItemListToReturn.size()+" fotek. Prosím nahrajte další, než nezbudou žádné.");
        }
        return mediaItemListToReturn;
    }

    public List<FileUpload> getListOfPictures(String server, int quantity) throws IOException {
        List<FileUpload> fileList = new ArrayList<>();
        //Odfiltrování jíž zobrazených fotek na serveru.
        System.out.println("itemy při for cyklu");
        for (int i = 0; i < quantity; i++) {
            fileList.add(updateAndFileUpload(server));
        }
        return fileList;
    }

    private FileUpload updateAndFileUpload(String server) {
        List<MediaItem> mediaItemList = getFilteredItems(server);
        MediaItem item = mediaItemList.get(mediaItemList.size()>1?new Random().nextInt(mediaItemList.size() - 1):0);
        System.out.println(item.getId());
        int itemIndex = items.indexOf(item);
        item = photosLibraryClient.updateMediaItemDescription(item, item.getDescription() + " " + server);
        item = photosLibraryClient.getMediaItem(item.getId());
        items.set(itemIndex, item);
        try {
            return getPhoto(item.getBaseUrl(), item.getFilename());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void cleanServerFromDescription(String server){
        List<MediaItem> mediaItemList = new ArrayList<>(items.stream().filter(mediaItem -> mediaItem.getDescription().contains(server)
        ).toList());
        System.out.println(mediaItemList.size());
        for (MediaItem item : mediaItemList) {
            System.out.println(item.getId());
            int itemIndex = items.indexOf(item);
            String description = item.getDescription().replace(server, "");
            item = photosLibraryClient.updateMediaItemDescription(item, description);
            item = photosLibraryClient.getMediaItem(item.getId());
            items.set(itemIndex, item);
        }
    }

    public FileUpload getUniquePicture(String server) throws ImagesNotFoundException {
        if (getFilteredItems(server).isEmpty()){
            throw new ImagesNotFoundException();
        }
        return updateAndFileUpload(server);
    }

    private FileUpload getPhoto(String baseUrl, String fileName) throws Exception {
        URI url = new URI(baseUrl);
        return FileUpload.fromData(url.toURL().openStream(), fileName);
    }

    //Nahrávač fotek
    public static void main(String[] args) throws GeneralSecurityException, IOException {
        GooglePhotos googlePhotos = new GooglePhotos();
        File dirFile=new File("Images");
        List<File> files = List.of(Objects.requireNonNull(dirFile.listFiles()));
        List<String> namesOfFolders=googlePhotos.items.stream().map(MediaItem::getFilename).toList();
        files=files.stream().filter(file -> !namesOfFolders.contains(file.getName())).toList();
        System.out.println(files.size());
        googlePhotos.uploadItems(files);
//        googlePhotos.cleanServerFromDescription("Klucííí");
    }

    public void uploadItems(List<File> directoryWithPictures) {
        List<List<NewMediaItem>> listsOfMediaItems = createMediaItems(directoryWithPictures);
        for (List<NewMediaItem> mediaItemList : listsOfMediaItems) {
            try {
                BatchCreateMediaItemsResponse response = photosLibraryClient.batchCreateMediaItems(mediaItemList);
                for (NewMediaItemResult itemsResponse : response.getNewMediaItemResultsList()) {
                    Status status = itemsResponse.getStatus();
                    if (status.getCode() == Code.OK_VALUE) {
                        System.out.println("Obrázek byl nahrán.");
                    } else {
                        System.out.println(status.getCode() + "\n" + status.getMessage());
                    }
                }
            } catch (ApiException e) {
                System.out.println(e);
            }
            System.out.println("Batch úspěšně nahraný.");
        }
    }

    private String getContentType(File file) throws IOException {
        // Možné v budoucnu upravit na vracení specifických contentType.
        return Files.probeContentType(file.getAbsoluteFile().toPath());
    }

    private List<List<NewMediaItem>> createMediaItems(List<File> directoryWithPictures) {
        List<List<NewMediaItem>> toReturn = new ArrayList<>();
        File file;
        assert directoryWithPictures != null;
        int iterator = directoryWithPictures.size() / 50;
        if (directoryWithPictures.size() % 50 != 0) {
            iterator++;
        }
        for (int i = 0; i < iterator; i++) {
            toReturn.add(new ArrayList<>());
        }
        int actualFileIterator = 0;
        for (int i = 0; i < iterator; i++) {
            // Open the file and automatically close it after upload
            for (int j = 0; j < 50; j++) {
                if (actualFileIterator==(directoryWithPictures.size()-1)){
                    break;
                }
                file = directoryWithPictures.get(actualFileIterator);

                actualFileIterator++;
                System.out.println("Uploaduji soubor:" + file.getName() + " actualIndex:" + actualFileIterator);
                uploadPictures(toReturn.get(i),file);
            }
        }
        return toReturn;
    }

    private void uploadPictures(List<NewMediaItem> mediaItemList, File file){
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            // Create a new upload request
            UploadMediaItemRequest uploadRequest =
                    UploadMediaItemRequest.newBuilder()
                            .setMimeType(getContentType(file))
                            // The file to upload
                            .setDataFile(randomAccessFile)
                            .build();
            // Upload and capture the response
            UploadMediaItemResponse uploadResponse = photosLibraryClient.uploadMediaItem(uploadRequest);
            if (uploadResponse.getError().isPresent()) {
                // If the upload results in an error, handle it
                UploadMediaItemResponse.Error error = uploadResponse.getError().get();
                System.out.println(error);
            } else {
                // If the upload is successful, get the uploadToken
                mediaItemList.add(NewMediaItemFactory
                        .createNewMediaItem(uploadResponse.getUploadToken().get(), file.getName(), "Description"));
                // Use this upload token to create a media item
            }
        } catch (ApiException e) {
            System.out.println(e);
            // Handle error
        } catch (IOException e) {
            System.out.println(e);
            // Error accessing the local file
        }
    }
}