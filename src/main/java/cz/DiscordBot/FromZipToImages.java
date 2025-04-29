package cz.DiscordBot;

import java.io.*;
        import java.nio.file.*;
        import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FromZipToImages {

    /**
     * Najde všechny ZIP soubory ve složce %folderWithZips%, z každého extrahuje obrázky (jpg, png, jpeg, gif, bmp)
     * a zkopíruje je do složky "Images" jen pokud se tam již nenacházejí.
     */
    public void copyImagesFromZips(String folderWithZips) {
        Path zipsDir = Paths.get(folderWithZips);
        Path projectRoot = Paths.get("").toAbsolutePath();
        Path imagesDir = projectRoot.resolve("Images");

        try {
            // Vytvoří složku Images pokud neexistuje
            if (!Files.exists(imagesDir)) {
                Files.createDirectories(imagesDir);
            }

            // Všechny zip soubory v dané složce
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(zipsDir, "*.zip")) {
                for (Path zipPath : stream) {
                    extractImagesFromZip(zipPath, imagesDir);
                }
            }
        } catch (IOException e) {
            System.err.println("Chyba při práci se složkou ZIPů nebo složkou Images: " + e.getMessage());
        }
    }

    private void extractImagesFromZip(Path zipFile, Path imagesDir) {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && isImage(entry.getName())) {
                    // Název souboru bez cesty v ZIPu
                    String imgName = Paths.get(entry.getName()).getFileName().toString();
                    Path targetPath = imagesDir.resolve(imgName);

                    // Pokud obrázek neexistuje, zkopíruj ho
                    if (!Files.exists(targetPath)) {
                        try (OutputStream os = Files.newOutputStream(targetPath)) {
                            byte[] buffer = new byte[4096];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                os.write(buffer, 0, len);
                            }
                        }
                    }
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            System.err.println("Chyba při extrakci z " + zipFile.getFileName() + ": " + e.getMessage());
        }
    }

    private boolean isImage(String fileName) {
        String name = fileName.toLowerCase();
        return name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png")
                || name.endsWith(".gif")
                || name.endsWith(".bmp");
    }

    public static void main(String[] args) {
        FromZipToImages helper = new FromZipToImages();
        helper.copyImagesFromZips("Zips"); // např. "C:/data/zips"
    }
}