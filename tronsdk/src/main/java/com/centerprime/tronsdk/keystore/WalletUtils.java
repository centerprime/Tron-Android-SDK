package com.centerprime.tronsdk.keystore;

import android.content.Context;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.centerprime.tronsdk.common.crypto.ECKey;
import com.centerprime.tronsdk.common.utils.Utils;
import com.centerprime.tronsdk.core.exception.CipherException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static android.content.Context.MODE_PRIVATE;

/**
 * Utility functions for working with Wallet files.
 */
public class WalletUtils {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public static String generateFullNewWalletFile(byte[] password, File destinationDirectory)
      throws NoSuchAlgorithmException, NoSuchProviderException,
      InvalidAlgorithmParameterException, CipherException, IOException {

    return generateNewWalletFile(password, destinationDirectory, true);
  }

  public static String generateLightNewWalletFile(byte[] password, File destinationDirectory)
      throws NoSuchAlgorithmException, NoSuchProviderException,
      InvalidAlgorithmParameterException, CipherException, IOException {

    return generateNewWalletFile(password, destinationDirectory, false);
  }

  public static String generateNewWalletFile(
      byte[] password, File destinationDirectory, boolean useFullScrypt)
      throws CipherException, IOException, InvalidAlgorithmParameterException,
      NoSuchAlgorithmException, NoSuchProviderException {

    ECKey ecKeyPair = new ECKey(Utils.getRandom());
    return generateWalletFile(password, ecKeyPair, destinationDirectory, useFullScrypt);
  }

  public static String generateWalletFile(
      byte[] password, ECKey ecKeyPair, File destinationDirectory, boolean useFullScrypt)
      throws CipherException, IOException {

    WalletFile walletFile;
    if (useFullScrypt) {
      walletFile = Wallet.createStandard(password, ecKeyPair);
    } else {
      walletFile = Wallet.createLight(password, ecKeyPair);
    }

    String fileName = getWalletFileName(walletFile);
    File destination = new File(destinationDirectory, fileName);

    objectMapper.writeValue(destination, walletFile);

    return fileName;
  }

  public static void updateWalletFile(
      byte[] password, ECKey ecKeyPair, String keystore, Context c,  boolean useFullScrypt)
      throws CipherException, IOException {

    FileInputStream fis =  c.openFileInput(keystore);
    WalletFile walletFile = objectMapper.readValue(fis, WalletFile.class);
    fis.close();
    if (useFullScrypt) {
      walletFile = Wallet.createStandard(password, ecKeyPair);
    } else {
      walletFile = Wallet.createLight(password, ecKeyPair);
    }


    FileOutputStream fos = c.openFileOutput(keystore, MODE_PRIVATE);
    objectMapper.writeValue(fos, walletFile);
    fos.close();
  }

  public static String generateWalletFile(WalletFile walletFile, File destinationDirectory , Context context)
      throws IOException {
    String filename = getWalletFileName(walletFile);

    String filepath =  "tron_" + filename;
    FileOutputStream fos = context.openFileOutput(filepath, MODE_PRIVATE);

    objectMapper.writeValue(fos, walletFile);
    return filepath;
  }

  public static Credentials loadCredentialsAndroie(byte[] password, String fileSource, Context c)
          throws IOException, CipherException
  {
    FileInputStream fis =  c.openFileInput(fileSource);
    WalletFile walletFile = objectMapper.readValue(fis, WalletFile.class);
    return Credentials.create(Wallet.decrypt(password, walletFile));
  }

  public static Credentials loadCredentials(byte[] password, File source)
      throws IOException, CipherException {
    WalletFile walletFile = objectMapper.readValue(source, WalletFile.class);
    return Credentials.create(Wallet.decrypt(password, walletFile));
  }

  public static WalletFile loadWalletFile(File source) throws IOException {
   return objectMapper.readValue(source, WalletFile.class);
  }

  public static WalletFile loadWalletFileAndroid(String source, Context c) throws IOException {
    FileInputStream fis =  c.openFileInput(source);
    return objectMapper.readValue(fis, WalletFile.class);
  }

  public static WalletFile loadFromKeystore(String keystore, Context c) throws IOException {
    return objectMapper.readValue(keystore, WalletFile.class);
  }

  private static String getWalletFileName(WalletFile walletFile) {
    DateTimeFormatter format = DateTimeFormatter.ofPattern(
        "'UTC--'yyyy-MM-dd'T'HH-mm-ss.nVV'--'");
    ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

    return walletFile.getAddress().toLowerCase() + ".json";
  }

  public static String getDefaultKeyDirectory() {
    return getDefaultKeyDirectory(System.getProperty("os.name"));
  }

  static String getDefaultKeyDirectory(String osName1) {
    String osName = osName1.toLowerCase();

    if (osName.startsWith("mac")) {
      return String.format(
          "%s%sLibrary%sEthereum", System.getProperty("user.home"), File.separator,
          File.separator);
    } else if (osName.startsWith("win")) {
      return String.format("%s%sEthereum", System.getenv("APPDATA"), File.separator);
    } else {
      return String.format("%s%s.ethereum", System.getProperty("user.home"), File.separator);
    }
  }

  public static String getTestnetKeyDirectory() {
    return String.format(
        "%s%stestnet%skeystore", getDefaultKeyDirectory(), File.separator, File.separator);
  }

  public static String getMainnetKeyDirectory() {
    return String.format("%s%skeystore", getDefaultKeyDirectory(), File.separator);
  }

}
