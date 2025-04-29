package cz.DiscordBot;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class DataFromFilesLoader {
    public static String TOKEN_FOLDER(){return new File(System.getProperty("user.dir")).getParentFile().getAbsolutePath()+File.separator+"tokenFolder.txt";} //jedná se o folder s jedním řádkem, kde bude token na discord bota.
    public static String DAILY_SEND_CONFIG(){return new File(System.getProperty("user.dir")).getParentFile().getAbsolutePath()+File.separator+"dailySendConfig.txt";}
    public static String GOOGLE_API_FOLDER(){return new File(System.getProperty("user.dir")).getParentFile().getAbsolutePath()+File.separator+"googleApi.json";}
    public static String GOOGLE_REFRESH_TOKEN_FOLDER(){return new File(System.getProperty("user.dir")).getParentFile().getAbsolutePath() + File.separator + "refreshToken.txt";}

    public String getTokenFromFolder() {
        try (Scanner sc = new Scanner(new File(TOKEN_FOLDER()))) {
            //Jelikož se nepředpokládá, že by jsme měli v souboru tokenů víc tokenů, tak rovnou posílám první řádek.
            return sc.nextLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
    }
            }
    }
