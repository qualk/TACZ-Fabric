package com.tacz.guns.resource.loader.asset;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.tacz.guns.GunMod;
import com.tacz.guns.resource.CommonAssetManager;
import com.tacz.guns.resource.CommonGunPackLoader;
import com.tacz.guns.resource.network.CommonGunPackNetwork;
import com.tacz.guns.resource.network.DataType;
import com.tacz.guns.util.IOReader;
import com.tacz.guns.util.TacPathVisitor;
import net.minecraft.util.Identifier;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AllowAttachmentTagsLoader {
    private static final Marker MARKER = MarkerFactory.getMarker("AllowAttachmentTagsLoader");
    private static final Pattern TAGS_PATTERN = Pattern.compile("^(\\w+)/tags/attachments/allow_attachments/(\\w+)\\.json$");

    public static boolean load(ZipFile zipFile, String zipPath) {
        Matcher matcher = TAGS_PATTERN.matcher(zipPath);
        if (matcher.find()) {
            String namespace = TacPathVisitor.checkNamespace(matcher.group(1));
            String path = matcher.group(2);
            ZipEntry entry = zipFile.getEntry(zipPath);
            if (entry == null) {
                GunMod.LOGGER.warn(MARKER, "{} file don't exist", zipPath);
                return false;
            }
            try (InputStream stream = zipFile.getInputStream(entry)) {
                Identifier registryName = new Identifier(namespace, path);
                String json = IOReader.toString(stream, StandardCharsets.UTF_8);
                loadFromJsonString(registryName, json);
                CommonGunPackNetwork.addData(DataType.ALLOW_ATTACHMENT_TAGS, registryName, json);
                return true;
            } catch (IOException | JsonSyntaxException | JsonIOException exception) {
                GunMod.LOGGER.warn(MARKER, "Failed to read data file: {}, entry: {}", zipFile, entry);
                exception.printStackTrace();
            }
        }
        return false;
    }

    public static void load(File root) {
        Path filePath = root.toPath().resolve("tags/attachments/allow_attachments");
        if (Files.isDirectory(filePath)) {
            TacPathVisitor visitor = new TacPathVisitor(filePath.toFile(), root.getName(), ".json", (id, file) -> {
                try (InputStream stream = Files.newInputStream(file)) {
                    String json = IOReader.toString(stream, StandardCharsets.UTF_8);
                    loadFromJsonString(id, json);
                    CommonGunPackNetwork.addData(DataType.ALLOW_ATTACHMENT_TAGS, id, json);
                } catch (IOException | JsonSyntaxException | JsonIOException exception) {
                    GunMod.LOGGER.warn(MARKER, "Failed to read data file: {}", file);
                    exception.printStackTrace();
                }
            });
            try {
                Files.walkFileTree(filePath, EnumSet.noneOf(FileVisitOption.class), 1, visitor);
            } catch (IOException e) {
                GunMod.LOGGER.warn(MARKER, "Failed to walk file tree: {}", filePath);
                e.printStackTrace();
            }
        }
    }

    public static void loadFromJsonString(Identifier id, String json) {
        List<String> tags = CommonGunPackLoader.GSON.fromJson(json, new TypeToken<>() {
        });
        CommonAssetManager.INSTANCE.putAllowAttachmentTags(id, tags);
    }
}
