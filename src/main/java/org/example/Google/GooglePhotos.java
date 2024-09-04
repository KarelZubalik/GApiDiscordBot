package org.example.Google;


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
import net.dv8tion.jda.api.utils.FileUpload;
import org.example.Google.Factories.PhotosLibraryClientFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.example.DataFromFilesLoader.GOOGLE_API_FOLDER;

public class GooglePhotos {
    List<MediaItem> items;

    private static final List<String> REQUIRED_SCOPES =
            ImmutableList.of("https://www.googleapis.com/auth/photoslibrary.readonly","https://www.googleapis.com/auth/photoslibrary.appendonly","https://www.googleapis.com/auth/photoslibrary.edit.appcreateddata");
    private final PhotosLibraryClient photosLibraryClient;

    public GooglePhotos() throws GeneralSecurityException, IOException {
        try {
            System.out.println(GOOGLE_API_FOLDER());
            photosLibraryClient = PhotosLibraryClientFactory.createClient(GOOGLE_API_FOLDER(),REQUIRED_SCOPES);
            items= new ArrayList<>();
            inizializeItemsIfNeeded();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private void inizializeItemsIfNeeded(){
        if (items.isEmpty()) {
            InternalPhotosLibraryClient.ListMediaItemsPagedResponse response = photosLibraryClient.listMediaItems(ListMediaItemsRequest.newBuilder().setPageSize(100).build());
            System.out.println("itemy při iterateAll");
            for (MediaItem item : response.iterateAll()) {
                System.out.println(item.getId());
                items.add(item);
            }
        }
    }

    private List<MediaItem> getFilteredItems(String server){
        return new ArrayList<>(items.stream().filter(mediaItem -> !mediaItem.getDescription().contains(server)
        ).toList());
    }

    public List<FileUpload> getListOfPictures(String server,int quantity) throws IOException {
        List<FileUpload> fileList = new ArrayList<>();
        //Odfiltrování jíž zobrazených fotek na serveru.
        System.out.println("itemy při for cyklu");
        for (int i = 0; i < quantity; i++) {
            fileList.add(updateAndFileUpload(server));
        }
        return fileList;
    }
    private FileUpload updateAndFileUpload( String server){
        List<MediaItem> mediaItemList=getFilteredItems(server);
        MediaItem item=mediaItemList.get(new Random().nextInt(mediaItemList.size()-1));
        System.out.println(item.getId());
        int itemIndex=items.indexOf(item);
        item=photosLibraryClient.updateMediaItemDescription(item, item.getDescription()+" "+server);
        item = photosLibraryClient.getMediaItem(item.getId());
        items.set(itemIndex,item);
        try {
         return getPhoto(item.getBaseUrl(), item.getFilename());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public FileUpload getUniquePicture(String server){
        getFilteredItems(server);
        return updateAndFileUpload(server);
    }

    private FileUpload getPhoto(String baseUrl, String fileName) throws Exception {
        URI url = new URI(baseUrl);
        return FileUpload.fromData(url.toURL().openStream(), fileName);
    }

    //Nahrávač fotek
    public static void main(String[] args) throws GeneralSecurityException, IOException {
        GooglePhotos googlePhotos = new GooglePhotos();
        googlePhotos.uploadItems(new File("SpecDirectionFolder"));
    }

    public void uploadItems(File directoryWithPictures){
        List<List<NewMediaItem>> listsOfMediaItems = createMediaItems(directoryWithPictures);
        for (List<NewMediaItem> mediaItemList:listsOfMediaItems) {
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

    private List<List<NewMediaItem>> createMediaItems(File directoryWithPictures){
        List<List<NewMediaItem>> toReturn=new ArrayList<>();
        File[] files = directoryWithPictures.listFiles();
        File file;
        assert files != null;
        int iterator=files.length/50;
        if (files.length%50!=0){
            iterator++;
        }
        for (int i = 0; i < iterator; i++) {
            toReturn.add(new ArrayList<>());
        }
        int actualFileIterator=0;
        for (int i = 0; i < iterator; i++) {
            // Open the file and automatically close it after upload
            for (int j = 0; j < 50; j++) {
                try {
                    file = files[actualFileIterator];
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("Vyskočila exception. Ukončiju cyklus na nahrávání souborů.");
                    break;
                }
                actualFileIterator++;
                System.out.println("Uploaduji soubor:" + file.getName()+" actualIndex:"+actualFileIterator);
                try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
                    // Create a new upload request
                    UploadMediaItemRequest uploadRequest =
                            UploadMediaItemRequest.newBuilder()
                                    // The media type (e.g. "image/png")
                                    .setMimeType("jpg")
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
                        toReturn.get(i).add(NewMediaItemFactory
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
        return toReturn;
    }
}

