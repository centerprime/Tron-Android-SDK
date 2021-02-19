package com.centerprime.tronsdk.walletcli;

import android.util.Log;

import com.centerprime.tronsdk.api.GrpcAPI.AccountNetMessage;
import com.centerprime.tronsdk.api.GrpcAPI.AccountResourceMessage;
import com.centerprime.tronsdk.api.GrpcAPI.AddressPrKeyPairMessage;
import com.centerprime.tronsdk.api.GrpcAPI.AssetIssueList;
import com.centerprime.tronsdk.api.GrpcAPI.BlockExtention;
import com.centerprime.tronsdk.api.GrpcAPI.NumberMessage;
import com.centerprime.tronsdk.common.utils.AbiUtil;
import com.centerprime.tronsdk.common.utils.ByteArray;
import com.centerprime.tronsdk.common.utils.Utils;
import com.centerprime.tronsdk.core.exception.CancelException;
import com.centerprime.tronsdk.core.exception.CipherException;
import com.centerprime.tronsdk.core.exception.EncodingException;
import com.centerprime.tronsdk.keystore.StringUtils;
import com.centerprime.tronsdk.protos.Contract.AssetIssueContract;
import com.centerprime.tronsdk.protos.Protocol.Account;
import com.centerprime.tronsdk.protos.Protocol.Block;
import com.centerprime.tronsdk.protos.Protocol.SmartContract;
import com.centerprime.tronsdk.walletserver.WalletApi;

import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

//import com.beust.jcommander.JCommander;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class Client {

  //private static final Logger logger = LoggerFactory.getLogger("Client");
  private WalletApiWrapper walletApiWrapper = new WalletApiWrapper();

  private char[] inputPassword2Twice() throws IOException {
    char[] password0;
    while (true) {
      System.out.println("Please input password.");
      password0 = Utils.inputPassword(true);
      System.out.println("Please input password again.");
      char[] password1 = Utils.inputPassword(true);
      boolean flag = Arrays.equals(password0, password1);
      StringUtils.clear(password1);
      if (flag) {
        break;
      }
      System.out.println("The passwords do not match, please input again.");
    }
    return password0;
  }

  private byte[] inputPrivateKey() throws IOException {
    byte[] temp = new byte[128];
    byte[] result = null;
    System.out.println("Please input private key.");
    while (true) {
      int len = System.in.read(temp, 0, temp.length);
      if (len >= 64) {
        byte[] privateKey = Arrays.copyOfRange(temp, 0, 64);
        result = StringUtils.hexs2Bytes(privateKey);
        StringUtils.clear(privateKey);
        if (WalletApi.priKeyValid(result)) {
          break;
        }
      }
      StringUtils.clear(result);
      System.out.println("Invalid private key, please input again.");
    }
    StringUtils.clear(temp);
    return result;
  }

  private byte[] inputPrivateKey64() throws IOException {
    Decoder decoder = Base64.getDecoder();
    byte[] temp = new byte[128];
    byte[] result;
    System.out.println("Please input private key by base64.");
    while (true) {
      int len = System.in.read(temp, 0, temp.length);
      if (len >= 44) {
        byte[] priKey64 = Arrays.copyOfRange(temp, 0, 44);
        result = decoder.decode(priKey64);
        StringUtils.clear(priKey64);
        if (WalletApi.priKeyValid(result)) {
          break;
        }
      }
      System.out.println("Invalid base64 private key, please input again.");
    }
    StringUtils.clear(temp);
    return result;
  }

  private void registerWallet() throws CipherException, IOException {
    char[] password = inputPassword2Twice();
    String fileName = walletApiWrapper.registerWallet(password,null);
    StringUtils.clear(password);

    if (null == fileName) {
      Log.d("tag","Register wallet failed !!");
      return;
    }
    Log.d("tag","Register a wallet successful, keystore file name is " + fileName);
  }

  private void importWallet() throws CipherException, IOException {
    char[] password = inputPassword2Twice();
    byte[] priKey = inputPrivateKey();

    String fileName = walletApiWrapper.importWallet(password, priKey);
    StringUtils.clear(password);
    StringUtils.clear(priKey);

    if (null == fileName) {
      System.out.println("Import wallet failed !!");
      return;
    }
    System.out.println("Import a wallet successful, keystore file name is " + fileName);
  }

  private void importwalletByBase64() throws CipherException, IOException {
    char[] password = inputPassword2Twice();
    byte[] priKey = inputPrivateKey64();

    String fileName = walletApiWrapper.importWallet(password, priKey);
    StringUtils.clear(password);
    StringUtils.clear(priKey);

    if (null == fileName) {
      System.out.println("Import wallet failed !!");
      return;
    }
    System.out.println("Import a wallet successful, keystore file name is " + fileName);
  }

  private void changePassword() throws IOException, CipherException {
    System.out.println("Please input old password.");
    char[] oldPassword = Utils.inputPassword(false);
    System.out.println("Please input new password.");
    char[] newPassword = inputPassword2Twice();

    StringUtils.clear(oldPassword);
    StringUtils.clear(newPassword);
  }

  private void login() throws IOException, CipherException {
    System.out.println("Please input your password.");
    char[] password = Utils.inputPassword(false);

    boolean result = walletApiWrapper.login(password);
    StringUtils.clear(password);

    if (result) {
      System.out.println("Login successful !!!");
    } else {
      System.out.println("Login failed !!!");
    }
  }

  private void logout() {
    walletApiWrapper.logout();
    System.out.println("Logout successful !!!");
  }

  private void backupWallet() throws IOException, CipherException {
    System.out.println("Please input your password.");
    char[] password = Utils.inputPassword(false);

  }

  private void backupWallet2Base64() throws IOException, CipherException {
    System.out.println("Please input your password.");
    char[] password = Utils.inputPassword(false);

    /*
    byte[] priKey = walletApiWrapper.backupWallet(password);
    StringUtils.clear(password);

    if (!ArrayUtils.isEmpty(priKey)) {
      Encoder encoder = Base64.getEncoder();
      byte[] priKey64 = encoder.encode(priKey);
      StringUtils.clear(priKey);
      System.out.println("BackupWallet successful !!");
      for (int i = 0; i < priKey64.length; i++) {
        System.out.print((char) priKey64[i]);
      }
      System.out.println();
      StringUtils.clear(priKey64);
    }
    */
  }

  private void getAddress() {
    String address = walletApiWrapper.getAddress();
    if (address != null) {
      Log.d("tag","GetAddress successful !!");
      Log.d("tag","address = " + address);
    }
  }

  private void getBalance() {
    Account account = walletApiWrapper.queryAccount();
    if (account == null) {
      Log.d("tag","GetBalance failed !!!!");

    } else {
      long balance = account.getBalance();
      Log.d("tag","Balance = " + balance);
    }
  }

  private void getAccount(String[] parameters) {
    if (parameters == null || parameters.length != 1) {
      System.out.println("GetAccount needs 1 parameter like the following: ");
      System.out.println("GetAccount Address ");
      return;
    }
    String address = parameters[0];
    byte[] addressBytes = WalletApi.decodeFromBase58Check(address);
    if (addressBytes == null) {
      return;
    }

      Account account = WalletApi.queryAccount(addressBytes);
    if (account == null) {
      Log.d("tag","GetAccount failed !!!!");
    } else {
      Log.d("tag","\n" + Utils.printAccount(account));
    }
  }

  private void getAccountById(String[] parameters) {
    if (parameters == null || parameters.length != 1) {
      System.out.println("GetAccountById needs 1 parameter like the following: ");
      System.out.println("GetAccountById accountId ");
      return;
    }
    String accountId = parameters[0];

    Account account = WalletApi.queryAccountById(accountId);
    if (account == null) {
      Log.d("tag","GetAccountById failed !!!!");
    } else {
      Log.d("tag","\n" + Utils.printAccount(account));
    }
  }


  private void updateAccount(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null || parameters.length != 1) {
      System.out.println("UpdateAccount needs 1 parameter like the following: ");
      System.out.println("UpdateAccount AccountName ");
      return;
    }

    String accountName = parameters[0];
    byte[] accountNameBytes = ByteArray.fromString(accountName);

    boolean ret = walletApiWrapper.updateAccount(accountNameBytes);
    if (ret) {
      Log.d("tag","Update Account successful !!!!");
    } else {
      Log.d("tag","Update Account failed !!!!");
    }
  }

  private void setAccountId(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null || parameters.length != 1) {
      System.out.println("SetAccountId needs 1 parameter like the following: ");
      System.out.println("SetAccountId AccountId ");
      return;
    }

    String accountId = parameters[0];
    byte[] accountIdBytes = ByteArray.fromString(accountId);

    boolean ret = walletApiWrapper.setAccountId(accountIdBytes);
    if (ret) {
      Log.d("tag","Set AccountId successful !!!!");
    } else {
      Log.d("tag","Set AccountId failed !!!!");
    }
  }


  private void updateAsset(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null || parameters.length != 4) {
      System.out.println("UpdateAsset needs 4 parameters like the following: ");
      System.out.println("UpdateAsset newLimit newPublicLimit description url");
      return;
    }

    String newLimitString = parameters[0];
    String newPublicLimitString = parameters[1];
    String description = parameters[2];
    String url = parameters[3];

    byte[] descriptionBytes = ByteArray.fromString(description);
    byte[] urlBytes = ByteArray.fromString(url);
    long newLimit = new Long(newLimitString);
    long newPublicLimit = new Long(newPublicLimitString);

  }

  private void getAssetIssueByAccount(String[] parameters) {
    if (parameters == null || parameters.length != 1) {
      System.out.println("GetAssetIssueByAccount needs 1 parameter like following: ");
      System.out.println("GetAssetIssueByAccount Address ");
      return;
    }
    String address = parameters[0];
    byte[] addressBytes = WalletApi.decodeFromBase58Check(address);
    if (addressBytes == null) {
      return;
    }

    Optional<AssetIssueList> result = WalletApi.getAssetIssueByAccount(addressBytes);
    if (result.isPresent()) {
      AssetIssueList assetIssueList = result.get();
      Log.d("tag",Utils.printAssetIssueList(assetIssueList));
    } else {
      Log.d("tag","GetAssetIssueByAccount " + " failed !!");
    }
  }

  private void getAccountNet(String[] parameters) {
    if (parameters == null || parameters.length != 1) {
      System.out.println("GetAccountNet needs 1 parameter like following: ");
      System.out.println("GetAccountNet Address ");
      return;
    }
    String address = parameters[0];
    byte[] addressBytes = WalletApi.decodeFromBase58Check(address);
    if (addressBytes == null) {
      return;
    }

    AccountNetMessage result = WalletApi.getAccountNet(addressBytes);
    if (result == null) {
      Log.d("tag","GetAccountNet " + " failed !!");
    } else {
      Log.d("tag","\n" + Utils.printAccountNet(result));
    }
  }

  private void getAccountResource(String[] parameters) {
    if (parameters == null || parameters.length != 1) {
      System.out.println("getAccountResource needs 1 parameter like following: ");
      System.out.println("getAccountResource Address ");
      return;
    }
    String address = parameters[0];
    byte[] addressBytes = WalletApi.decodeFromBase58Check(address);
    if (addressBytes == null) {
      return;
    }

    AccountResourceMessage result = WalletApi.getAccountResource(addressBytes);
    if (result == null) {
      Log.d("tag","getAccountResource " + " failed !!");
    } else {
      Log.d("tag","\n" + Utils.printAccountResourceMessage(result));
    }
  }

  // In 3.2 version, this function will return null if there are two or more asset with the same token name,
  // so please use getAssetIssueById or getAssetIssueListByName.
  // This function just remains for compatibility.
  private void getAssetIssueByName(String[] parameters) {
    if (parameters == null || parameters.length != 1) {
      System.out.println("GetAssetIssueByName needs 1 parameter like following: ");
      System.out.println("GetAssetIssueByName AssetName ");
      return;
    }
    String assetName = parameters[0];

    AssetIssueContract assetIssueContract = WalletApi.getAssetIssueByName(assetName);
    if (assetIssueContract != null) {
      Log.d("tag","\n" + Utils.printAssetIssue(assetIssueContract));
    } else {
      Log.d("tag","getAssetIssueByName " + " failed !!");
    }
  }

  private void getAssetIssueById(String[] parameters) {
    if (parameters == null || parameters.length != 1) {
      System.out.println("getAssetIssueById needs 1 parameter like following: ");
      System.out.println("getAssetIssueById AssetId ");
      return;
    }
    String assetId = parameters[0];

    AssetIssueContract assetIssueContract = WalletApi.getAssetIssueById(assetId);
    if (assetIssueContract != null) {
      Log.d("tag","\n" + Utils.printAssetIssue(assetIssueContract));
    } else {
      Log.d("tag","getAssetIssueById " + " failed !!");
    }
  }

  private void sendCoin(String[] parameters) throws IOException, CipherException, CancelException {
    if (parameters == null || parameters.length != 2) {
      System.out.println("SendCoin needs 2 parameters like following: ");
      System.out.println("SendCoin ToAddress Amount");
      return;
    }

    String toAddress = parameters[0];
    String amountStr = parameters[1];
    long amount = new Long(amountStr);

    boolean result = walletApiWrapper.sendCoin(toAddress, amount);
    if (result) {
      Log.d("tag","Send " + amount + " drop to " + toAddress + " successful !!");
    } else {
      Log.d("tag","Send " + amount + " drop to " + toAddress + " failed !!");
    }
  }

  private void testTransaction(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null || (parameters.length != 3 && parameters.length != 4)) {
      System.out.println("testTransaction needs 3 or 4 parameters using the following syntax: ");
      System.out.println("testTransaction ToAddress assertName times");
      System.out.println("testTransaction ToAddress assertName times interval");
      System.out.println("If needing transferAsset, assertName input null");
      return;
    }

    String toAddress = parameters[0];
    String assertName = parameters[1];
    String loopTime = parameters[2];
    int intervalInt = 0;//s
    if (parameters.length == 5) {
      String interval = parameters[4];
      intervalInt = Integer.parseInt(interval);//s
    }
    intervalInt *= 500; //ms
    long times = new Long(loopTime);

    for (int i = 1; i <= times; i++) {
      long amount = i;
      boolean result = walletApiWrapper.sendCoin(toAddress, amount);
      if (result) {
        Log.d("tag","Send " + amount + " drop to " + toAddress + " successful !!");
        if (intervalInt > 0) {
          try {
            Thread.sleep(intervalInt);
          } catch (Exception e) {
            e.printStackTrace();
            break;
          }
        }
      } else {
        Log.d("tag","Send " + amount + " drop to " + toAddress + " failed !!");
        break;
      }

      if (!"null".equalsIgnoreCase(assertName)) {
        result = walletApiWrapper.transferAsset(toAddress, assertName, amount);
        if (result) {
          Log.d("tag",
                  "transferAsset " + amount + assertName + " to " + toAddress + " successful !!");
          if (intervalInt > 0) {
            try {
              Thread.sleep(intervalInt);
            } catch (Exception e) {
              e.printStackTrace();
              break;
            }
          }
        } else {
          Log.d("tag","transferAsset " + amount + assertName + " to " + toAddress + " failed !!");
          break;
        }
      }
    }

  }

  private void transferAsset(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null || parameters.length != 3) {
      System.out.println("TransferAsset needs 3 parameters using the following syntax: ");
      System.out.println("TransferAsset ToAddress AssertName Amount");
      return;
    }

    String toAddress = parameters[0];
    String assertName = parameters[1];
    String amountStr = parameters[2];
    long amount = new Long(amountStr);

    boolean result = walletApiWrapper.transferAsset(toAddress, assertName, amount);
    if (result) {
      Log.d("tag","TransferAsset " + amount + " to " + toAddress + " successful !!");
    } else {
      Log.d("tag","TransferAsset " + amount + " to " + toAddress + " failed !!");
    }
  }

  private void participateAssetIssue(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null || parameters.length != 3) {
      System.out.println("ParticipateAssetIssue needs 3 parameters using the following syntax: ");
      System.out.println("ParticipateAssetIssue ToAddress AssetName Amount");
      return;
    }

    String toAddress = parameters[0];
    String assertName = parameters[1];
    String amountStr = parameters[2];
    long amount = Long.parseLong(amountStr);

    boolean result = walletApiWrapper.participateAssetIssue(toAddress, assertName, amount);
    if (result) {
      Log.d("tag","ParticipateAssetIssue " + assertName + " " + amount + " from " + toAddress
          + " successful !!");
    } else {
      Log.d("tag","ParticipateAssetIssue " + assertName + " " + amount + " from " + toAddress
          + " failed !!");
    }
  }

  private void assetIssue(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null || parameters.length < 11 || (parameters.length & 1) == 0) {
      System.out
          .println("Use the assetIssue command for features that you require with below syntax: ");
      System.out.println(
          "AssetIssue AssetName TotalSupply TrxNum AssetNum Precision "
              + "StartDate EndDate Description Url FreeNetLimitPerAccount PublicFreeNetLimit "
              + "FrozenAmount0 FrozenDays0 ... FrozenAmountN FrozenDaysN");
      System.out
          .println(
              "TrxNum and AssetNum represents the conversion ratio of the tron to the asset.");
      System.out
          .println("The StartDate and EndDate format should look like 2018-3-1 2018-3-21 .");
      return;
    }

    String name = parameters[0];
    String totalSupplyStr = parameters[1];
    String trxNumStr = parameters[2];
    String icoNumStr = parameters[3];
    String precisionStr = parameters[4];
    String startYyyyMmDd = parameters[5];
    String endYyyyMmDd = parameters[6];
    String description = parameters[7];
    String url = parameters[8];
    String freeNetLimitPerAccount = parameters[9];
    String publicFreeNetLimitString = parameters[10];
    HashMap<String, String> frozenSupply = new HashMap<>();
    for (int i = 11; i < parameters.length; i += 2) {
      String amount = parameters[i];
      String days = parameters[i + 1];
      frozenSupply.put(days, amount);
    }

    long totalSupply = new Long(totalSupplyStr);
    int trxNum = new Integer(trxNumStr);
    int icoNum = new Integer(icoNumStr);
    int precision = new Integer(precisionStr);
    Date startDate = Utils.strToDateLong(startYyyyMmDd);
    Date endDate = Utils.strToDateLong(endYyyyMmDd);
    long startTime = startDate.getTime();
    long endTime = endDate.getTime();
    long freeAssetNetLimit = new Long(freeNetLimitPerAccount);
    long publicFreeNetLimit = new Long(publicFreeNetLimitString);

    boolean result = walletApiWrapper
        .assetIssue(name, totalSupply, trxNum, icoNum, precision, startTime, endTime,
            0, description, url, freeAssetNetLimit, publicFreeNetLimit, frozenSupply);
    if (result) {
      Log.d("tag","AssetIssue " + name + " successful !!");
    } else {
      Log.d("tag","AssetIssue " + name + " failed !!");
    }
  }

  private void createAccount(String[] parameters)
      throws CipherException, IOException, CancelException {
    if (parameters == null || parameters.length != 1) {
      System.out.println("CreateAccount needs 1 parameter using the following syntax: ");
      System.out.println("CreateAccount Address");
      return;
    }

    String address = parameters[0];

    boolean result = walletApiWrapper.createAccount(address);
    if (result) {
      Log.d("tag","CreateAccount " + " successful !!");
    } else {
      Log.d("tag","CreateAccount " + " failed !!");
    }
  }

  private void createWitness(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null || parameters.length != 1) {
      System.out.println("CreateWitness needs 1 parameter using the following syntax: ");
      System.out.println("CreateWitness Url");
      return;
    }

    String url = parameters[0];

    boolean result = walletApiWrapper.createWitness(url);
    if (result) {
      Log.d("tag","CreateWitness " + " successful !!");
    } else {
      Log.d("tag", "CreateWitness " + " failed !!");
    }
  }

  private void updateWitness(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null || parameters.length != 1) {
      System.out.println("updateWitness needs 1 parameter using the following syntax: ");
      System.out.println("updateWitness Url");
      return;
    }

    String url = parameters[0];

    boolean result = walletApiWrapper.updateWitness(url);
    if (result) {
      Log.d("tag","updateWitness " + " successful !!");
    } else {
      Log.d("tag", "updateWitness " + " failed !!");
    }
  }

  private void getBlock(String[] parameters) {
    long blockNum = -1;

    if (parameters == null || parameters.length == 0) {
      System.out.println("Get current block !!!!");
    } else {
      if (parameters.length != 1) {
        System.out.println("Getblock has too many parameters !!!");
        System.out.println("You can get current block using the following command:");
        System.out.println("Getblock");
        System.out.println("Or get block by number with the following syntax:");
        System.out.println("Getblock BlockNum");
      }
      blockNum = Long.parseLong(parameters[0]);
    }

    if (WalletApi.getRpcVersion() == 2) {
      BlockExtention blockExtention = walletApiWrapper.getBlock2(blockNum);
      if (blockExtention == null) {
        System.out.println("No block for num : " + blockNum);
        return;
      }
      System.out.println(Utils.printBlockExtention(blockExtention));
    } else {
      Block block = walletApiWrapper.getBlock(blockNum);
      if (block == null) {
        System.out.println("No block for num : " + blockNum);
        return;
      }
      System.out.println(Utils.printBlock(block));
    }
  }

  private void getTransactionCountByBlockNum(String[] parameters) {
    if (parameters.length != 1) {
      System.out.println("Too many parameters !!!");
      System.out.println("You need input number with the following syntax:");
      System.out.println("GetTransactionCountByBlockNum number");
    }
    long blockNum = Long.parseLong(parameters[0]);
    long count = walletApiWrapper.getTransactionCountByBlockNum(blockNum);
    System.out.println("The block contain " + count + " transactions");
  }

  private void voteWitness(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null || parameters.length < 2 || (parameters.length & 1) != 0) {
      System.out.println("Use VoteWitness command with below syntax: ");
      System.out.println("VoteWitness Address0 Count0 ... AddressN CountN");
      return;
    }

    HashMap<String, String> witness = new HashMap<String, String>();
    for (int i = 0; i < parameters.length; i += 2) {
      String address = parameters[i];
      String countStr = parameters[i + 1];
      witness.put(address, countStr);
    }

    boolean result = walletApiWrapper.voteWitness(witness);
    if (result) {
      Log.d("tag", "VoteWitness " + " successful !!");
    } else {
      Log.d("tag", "VoteWitness " + " failed !!");
    }
  }

  private void getTotalTransaction() {
    NumberMessage totalTransition = walletApiWrapper.getTotalTransaction();
    Log.d("tag", "The num of total transactions is : " + totalTransition.getNum());
  }

  private void getNextMaintenanceTime() {
    NumberMessage nextMaintenanceTime = walletApiWrapper.getNextMaintenanceTime();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String date = formatter.format(nextMaintenanceTime.getNum());
    Log.d("tag", "Next maintenance time is : " + date);
  }

  private void updateSetting(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null ||
        parameters.length < 2) {
      System.out.println("updateSetting needs 2 parameters like following: ");
      System.out.println("updateSetting contract_address consume_user_resource_percent");
      return;
    }

    byte[] contractAddress = WalletApi.decodeFromBase58Check(parameters[0]);
    long consumeUserResourcePercent = Long.valueOf(parameters[1]).longValue();
    if (consumeUserResourcePercent > 100 || consumeUserResourcePercent < 0) {
      System.out.println("consume_user_resource_percent must >= 0 and <= 100");
      return;
    }
    boolean result = walletApiWrapper.updateSetting(contractAddress, consumeUserResourcePercent);
    if (result) {
      System.out.println("update setting successfully");
    } else {
      System.out.println("update setting failed");
    }
  }

  private void updateEnergyLimit(String[] parameters)
      throws IOException, CipherException, CancelException {
    if (parameters == null ||
        parameters.length < 2) {
      System.out.println("updateEnergyLimit needs 2 parameters like following: ");
      System.out.println("updateEnergyLimit contract_address energy_limit");
      return;
    }

    byte[] contractAddress = WalletApi.decodeFromBase58Check(parameters[0]);
    long originEnergyLimit = Long.valueOf(parameters[1]).longValue();
    if (originEnergyLimit < 0) {
      System.out.println("origin_energy_limit need > 0 ");
      return;
    }
    boolean result = walletApiWrapper.updateEnergyLimit(contractAddress, originEnergyLimit);
    if (result) {
      System.out.println("update setting for origin_energy_limit successfully");
    } else {
      System.out.println("update setting for origin_energy_limit failed");
    }
  }

  private void triggerContract(String[] parameters)
      throws IOException, CipherException, CancelException, EncodingException {
    if (parameters == null ||
        parameters.length < 8) {
      System.out.println("TriggerContract needs 6 parameters like following: ");
      System.out.println(
          "TriggerContract contractAddress method args isHex fee_limit value token_value token_id(e.g: TRXTOKEN, use # if don't provided)");
      // System.out.println("example:\nTriggerContract password contractAddress method args value");
      return;
    }

    String contractAddrStr = parameters[0];
    String methodStr = parameters[1];
    String argsStr = parameters[2];
    boolean isHex = Boolean.valueOf(parameters[3]);
    long feeLimit = Long.valueOf(parameters[4]);
    long callValue = Long.valueOf(parameters[5]);
    long tokenCallValue = Long.valueOf(parameters[6]);
    String tokenId = parameters[7];
    if (argsStr.equalsIgnoreCase("#")) {
      argsStr = "";
    }
    if (tokenId.equalsIgnoreCase("#")) {
      tokenId = "";
    }
    byte[] input = Hex.decode(AbiUtil.parseMethod(methodStr, argsStr, isHex));
    byte[] contractAddress = WalletApi.decodeFromBase58Check(contractAddrStr);

    boolean result = walletApiWrapper
        .callContract(contractAddress, callValue, input, feeLimit, tokenCallValue, tokenId);
    if (result) {
      System.out.println("Broadcast the triggerContract successfully.\n"
          + "Please check the given transaction id to get the result on blockchain using getTransactionInfoById command");
    } else {
      System.out.println("Broadcast the triggerContract failed");
    }
  }

  private void getContract(String[] parameters) {
    if (parameters == null ||
        parameters.length != 1) {
      System.out.println("GetContract needs 1 parameter like following: ");
      System.out.println("GetContract contractAddress");
      return;
    }

    byte[] addressBytes = WalletApi.decodeFromBase58Check(parameters[0]);
    if (addressBytes == null) {
      System.out.println("GetContract: invalid address!");
      return;
    }

    SmartContract contractDeployContract = WalletApi.getContract(addressBytes);
    if (contractDeployContract != null) {
      System.out.println("contract :" + contractDeployContract.getAbi().toString());
      System.out.println("contract owner:" + WalletApi.encode58Check(contractDeployContract
          .getOriginAddress().toByteArray()));
      System.out.println("contract ConsumeUserResourcePercent:" + contractDeployContract
          .getConsumeUserResourcePercent());
      System.out.println("contract energy limit:" + contractDeployContract
          .getOriginEnergyLimit());
    } else {
      System.out.println("query contract failed!");
    }
  }

  private void generateAddress() {
    AddressPrKeyPairMessage result = walletApiWrapper.generateAddress();
    if (null != result) {
      System.out.println("Address: " + result.getAddress());
      System.out.println("PrivateKey: " + result.getPrivateKey());
      Log.d("tag", "GenerateAddress " + " successful !!");
    } else {
      Log.d("tag", "GenerateAddress " + " failed !!");
    }
  }

  private void help() {
    System.out.println("Help: List of Tron Wallet-cli commands");
    System.out.println(
        "For more information on a specific command, type the command and it will display tips");
    System.out.println("");
    System.out.println("ApproveProposal");
    System.out.println("AssetIssue");
    System.out.println("BackupWallet");
    System.out.println("BackupWallet2Base64");
    System.out.println("ChangePassword");
    System.out.println("CreateAccount");
    System.out.println("CreateProposal");
    System.out.println("CreateWitness");
    System.out.println("DeleteProposal");
    System.out.println(
        "DeployContract contractName ABI byteCode constructor params isHex fee_limit consume_user_resource_percent origin_energy_limit value token_value token_id <library:address,library:address,...>");
    System.out.println("ExchangeCreate");
    System.out.println("ExchangeInject");
    System.out.println("ExchangeTransaction");
    System.out.println("ExchangeWithdraw");
    System.out.println("FreezeBalance");
    System.out.println("GenerateAddress");
    System.out.println("GetAccount");
    System.out.println("GetAccountNet");
    System.out.println("GetAccountResource");
    System.out.println("GetAddress");
    System.out.println("GetAssetIssueByAccount");
    System.out.println("GetAssetIssueById");
    System.out.println("GetAssetIssueByName");
    System.out.println("GetAssetIssueListByName");
    System.out.println("GetBalance");
    System.out.println("GetBlock");
    System.out.println("GetBlockById");
    System.out.println("GetBlockByLatestNum");
    System.out.println("GetBlockByLimitNext");
    System.out.println("GetContract contractAddress");
    System.out.println("GetDelegatedResource");
    System.out.println("GetDelegatedResourceAccountIndex");
    System.out.println("GetExchange");
    System.out.println("GetNextMaintenanceTime");
    System.out.println("GetProposal");
    System.out.println("GetTotalTransaction");
    System.out.println("GetTransactionById");
    System.out.println("GetTransactionCountByBlockNum");
    System.out.println("GetTransactionInfoById");
    System.out.println("GetTransactionsFromThis");
    System.out.println("GetTransactionsToThis");
    System.out.println("ImportWallet");
    System.out.println("ImportWalletByBase64");
    System.out.println("ListAssetIssue");
    System.out.println("ListExchanges");
    System.out.println("ListExchangesPaginated");
    System.out.println("ListNodes");
    System.out.println("ListProposals");
    System.out.println("ListProposalsPaginated");
    System.out.println("ListWitnesses");
    System.out.println("Login");
    System.out.println("Logout");
    System.out.println("ParticipateAssetIssue");
    System.out.println("RegisterWallet");
    System.out.println("SendCoin");
    System.out.println("SetAccountId");
    System.out.println("TransferAsset");
    System.out.println("TriggerContract contractAddress method args isHex fee_limit value");
    System.out.println("UnfreezeAsset");
    System.out.println("UnfreezeBalance");
    System.out.println("UnfreezeAsset");
    System.out.println("UpdateAccount");
    System.out.println("UpdateAsset");
    System.out.println("UpdateEnergyLimit contract_address energy_limit");
    System.out.println("UpdateSetting contract_address consume_user_resource_percent");
    System.out.println("UpdateWitness");
    System.out.println("VoteWitness");
    System.out.println("WithdrawBalance");
//    System.out.println("buyStorage");
//    System.out.println("buyStorageBytes");
//    System.out.println("sellStorage");
//   System.out.println("GetAssetIssueListByTimestamp");
//   System.out.println("GetTransactionsByTimestamp");
//   System.out.println("GetTransactionsByTimestampCount");
//   System.out.println("GetTransactionsFromThisCount");
//   System.out.println("GetTransactionsToThisCount");
    System.out.println("Exit or Quit");

    System.out.println("Input any one of the listed commands, to display how-to tips.");
  }

  private String[] getCmd(String cmdLine) {
    if (cmdLine.indexOf("\"") < 0 || cmdLine.toLowerCase().startsWith("deploycontract")
        || cmdLine.toLowerCase().startsWith("triggercontract")) {
      return cmdLine.split("\\s+");
    }
    String[] strArray = cmdLine.split("\"");
    int num = strArray.length;
    int start = 0;
    int end = 0;
    if (cmdLine.charAt(0) == '\"') {
      start = 1;
    }
    if (cmdLine.charAt(cmdLine.length() - 1) == '\"') {
      end = 1;
    }
    if (((num + end) & 1) == 0) {
      return new String[]{"ErrorInput"};
    }

    List<String> cmdList = new ArrayList<>();
    for (int i = start; i < strArray.length; i++) {
      if ((i & 1) == 0) {
        cmdList.addAll(Arrays.asList(strArray[i].trim().split("\\s+")));
      } else {
        cmdList.add(strArray[i].trim());
      }
    }
    Iterator ito = cmdList.iterator();
    while (ito.hasNext()) {
      if (ito.next().equals("")) {
        ito.remove();
      }
    }
    String[] result = new String[cmdList.size()];
    return cmdList.toArray(result);
  }

  private void run() {
    Scanner in = new Scanner(System.in);
    System.out.println(" ");
    System.out.println("Welcome to Tron Wallet-Cli");
    System.out.println("Please type one of the following commands to proceed.");
    System.out.println("Login, RegisterWallet or ImportWallet");
    System.out.println(" ");
    System.out.println(
        "You may also use the Help command at anytime to display a full list of commands.");
    System.out.println(" ");
    while (in.hasNextLine()) {
      String cmd = "";
      try {
        String cmdLine = in.nextLine().trim();
        String[] cmdArray = getCmd(cmdLine);
        // split on trim() string will always return at the minimum: [""]
        cmd = cmdArray[0];
        if ("".equals(cmd)) {
          continue;
        }
        String[] parameters = Arrays.copyOfRange(cmdArray, 1, cmdArray.length);
        String cmdLowerCase = cmd.toLowerCase();

        switch (cmdLowerCase) {
          case "help": {
            help();
            break;
          }
          case "registerwallet": {
            registerWallet();
            break;
          }
          case "importwallet": {
            importWallet();
            break;
          }
          case "importwalletbybase64": {
            importwalletByBase64();
            break;
          }
          case "changepassword": {
            changePassword();
            break;
          }
          case "login": {
            login();
            break;
          }
          case "logout": {
            logout();
            break;
          }
          case "backupwallet": {
            backupWallet();
            break;
          }
          case "backupwallet2base64": {
            backupWallet2Base64();
            break;
          }
          case "getaddress": {
            getAddress();
            break;
          }
          case "getbalance": {
            getBalance();
            break;
          }
          case "getaccount": {
            getAccount(parameters);
            break;
          }
          case "getaccountbyid": {
            getAccountById(parameters);
            break;
          }
          case "updateaccount": {
            updateAccount(parameters);
            break;
          }
          case "setaccountid": {
            setAccountId(parameters);
            break;
          }
          case "updateasset": {
            updateAsset(parameters);
            break;
          }
          case "getassetissuebyaccount": {
            getAssetIssueByAccount(parameters);
            break;
          }
          case "getaccountnet": {
            getAccountNet(parameters);
            break;
          }
          case "getaccountresource": {
            getAccountResource(parameters);
            break;
          }
          case "getassetissuebyname": {
            getAssetIssueByName(parameters);
            break;
          }
          case "getassetissuebyid": {
            getAssetIssueById(parameters);
            break;
          }
          case "sendcoin": {
            sendCoin(parameters);
            break;
          }
          case "testtransaction": {
            testTransaction(parameters);
            break;
          }
          case "transferasset": {
            transferAsset(parameters);
            break;
          }
          case "participateassetissue": {
            participateAssetIssue(parameters);
            break;
          }
          case "assetissue": {
            assetIssue(parameters);
            break;
          }
          case "createaccount": {
            createAccount(parameters);
            break;
          }
          case "triggercontract": {
            triggerContract(parameters);
            break;
          }
          case "getcontract": {
            getContract(parameters);
            break;
          }
          case "generateaddress": {
            generateAddress();
            break;
          }
          case "exit":
          case "quit": {
            System.out.println("Exit !!!");
            return;
          }
          default: {
            System.out.println("Invalid cmd: " + cmd);
            help();
          }
        }
      } catch (CipherException e) {
        System.out.println(cmd + " failed!");
        System.out.println(e.getMessage());
      } catch (IOException e) {
        System.out.println(cmd + " failed!");
        System.out.println(e.getMessage());
      } catch (CancelException e) {
        System.out.println(cmd + " failed!");
        System.out.println(e.getMessage());
      } catch (Exception e) {
        System.out.println(cmd + " failed!");
        Log.d("tag", e.getMessage());
        e.printStackTrace();
      }
    }
  }

  public static void main(String[] args) {
    Client cli = new Client();
    cli.run();
  }
}
