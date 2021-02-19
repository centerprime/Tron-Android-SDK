package com.centerprime.tronsdk.walletcli;

import android.content.Context;
import android.util.Log;

import com.centerprime.tronsdk.abi.FunctionReturnDecoder;
import com.centerprime.tronsdk.abi.TypeReference;
import com.centerprime.tronsdk.abi.datatypes.Address;
import com.centerprime.tronsdk.abi.datatypes.Function;
import com.centerprime.tronsdk.abi.datatypes.generated.AbiTypes;
import com.centerprime.tronsdk.api.GrpcAPI;
import com.centerprime.tronsdk.api.GrpcAPI.AddressPrKeyPairMessage;
import com.centerprime.tronsdk.api.GrpcAPI.AssetIssueList;
import com.centerprime.tronsdk.api.GrpcAPI.BlockExtention;
import com.centerprime.tronsdk.api.GrpcAPI.ExchangeList;
import com.centerprime.tronsdk.api.GrpcAPI.NodeList;
import com.centerprime.tronsdk.api.GrpcAPI.ProposalList;
import com.centerprime.tronsdk.api.GrpcAPI.WitnessList;
import com.centerprime.tronsdk.common.crypto.ECKey;
import com.centerprime.tronsdk.common.crypto.Sha256Hash;
import com.centerprime.tronsdk.common.utils.AbiUtil;
import com.centerprime.tronsdk.common.utils.ByteArray;
import com.centerprime.tronsdk.common.utils.Hex2Decimal;
import com.centerprime.tronsdk.common.utils.StringTronUtil;
import com.centerprime.tronsdk.common.utils.TransactionUtils;
import com.centerprime.tronsdk.core.exception.CancelException;
import com.centerprime.tronsdk.core.exception.CipherException;
import com.centerprime.tronsdk.core.exception.EncodingException;
import com.centerprime.tronsdk.keystore.StringUtils;
import com.centerprime.tronsdk.protos.Contract;
import com.centerprime.tronsdk.protos.Contract.AssetIssueContract;
import com.centerprime.tronsdk.protos.Protocol;
import com.centerprime.tronsdk.protos.Protocol.Account;
import com.centerprime.tronsdk.protos.Protocol.Block;
import com.centerprime.tronsdk.protos.Protocol.ChainParameters;
import com.centerprime.tronsdk.protos.Protocol.Exchange;
import com.centerprime.tronsdk.protos.Protocol.Proposal;
import com.centerprime.tronsdk.walletserver.WalletApi;
import com.google.protobuf.ByteString;

import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WalletApiWrapper {

  public WalletApi wallet;
  public Context context;

  public ECKey ecKey = null;

  private static String TRANSFER_METHOD = "transfer(address,uint256)";

  public String registerWallet(char[] password, Context context)
          throws CipherException, IOException
  {
    if (!WalletApi.passwordValid(password)) {
      return null;
    }
    byte[] passwd = StringUtils.char2Byte(password);
    wallet = new WalletApi(passwd);
    wallet.setLogin();
    StringUtils.clear(passwd);
    return wallet.store2Keystore("", context);
  }

  public String importWallet(char[] password, byte[] priKey) throws CipherException, IOException {
    if (!WalletApi.passwordValid(password)) {
      return null;
    }
    if (!WalletApi.priKeyValid(priKey)) {
      return null;
    }
    byte[] passwd = StringUtils.char2Byte(password);
    wallet = new WalletApi(passwd, priKey);
    wallet.setLogin();
    StringUtils.clear(passwd);
    return wallet.getAddress().toString();
  }

  public boolean loginAndroid(char[] password, String keystoreName, Context c) throws IOException, CipherException {
        logout();
        wallet = WalletApi.loadWalletFromKeystoreAndroid(keystoreName, c);
        byte[] passwd = StringUtils.char2Byte(password);
      wallet.checkPassword(passwd);
      StringUtils.clear(passwd);

      if (wallet == null) {
        System.out.println("Warning: Login failed, Please registerWallet or importWallet first !!");
        return false;
      }
      wallet.setLogin();
      return true;
    }

  public boolean login(char[] password) throws IOException, CipherException {
    logout();
    wallet = WalletApi.loadWalletFromKeystore();

    byte[] passwd = StringUtils.char2Byte(password);
    wallet.checkPassword(passwd);
    StringUtils.clear(passwd);

    if (wallet == null) {
      System.out.println("Warning: Login failed, Please registerWallet or importWallet first !!");
      return false;
    }
    wallet.setLogin();
    return true;
  }

  public void logout() {
    if (wallet != null) {
      wallet.logout();
      wallet = null;
    }
  }

  //password is current, will be enc by password2.
  public byte[] backupWallet(char[] password, String keystore, Context c) throws IOException, CipherException {
    byte[] passwd = StringUtils.char2Byte(password);

    if (wallet == null || !wallet.isLoginState()) {
      wallet = WalletApi.loadWalletFromKeystoreAndroid(keystore, c);

      if (wallet == null) {
        StringUtils.clear(passwd);
        System.out.println("Warning: BackupWallet failed, no wallet can be backup !!");
        return null;
      }
    }

    byte[] privateKey = wallet.getPrivateBytes(passwd);
    StringUtils.clear(passwd);

    return privateKey;
  }

  public String getAddress() {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: GetAddress failed,  Please login first !!");
      return null;
    }

    return WalletApi.encode58Check(wallet.getAddress());
  }

  public Account queryAccount() {
    //if (wallet == null || !wallet.isLoginState())
    if (wallet==null)
    {
      Log.d("tag", "Warning: QueryAccount failed,  Please login first !!");
      return null;
    }

    return wallet.queryAccount();
  }

  public GrpcAPI.AccountNetMessage getAccount(byte[] addressBytes){
    return wallet.getAccount(addressBytes);
  }

  public boolean sendCoin(String toAddress, long amount)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: SendCoin failed,  Please login first !!");
      return false;
    }
    byte[] to = WalletApi.decodeFromBase58Check(toAddress);
    if (to == null) {
      return false;
    }

    return wallet.sendCoin(to, amount);
  }

  public boolean transferAsset(String toAddress, String assertName, long amount)
      throws IOException, CipherException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: TransferAsset failed,  Please login first !!");
      return false;
    }
    byte[] to = WalletApi.decodeFromBase58Check(toAddress);
    if (to == null) {
      return false;
    }

    return wallet.transferAsset(to, assertName.getBytes(), amount);
  }

  public boolean participateAssetIssue(String toAddress, String assertName,
      long amount) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: TransferAsset failed,  Please login first !!");
      return false;
    }
    byte[] to = WalletApi.decodeFromBase58Check(toAddress);
    if (to == null) {
      return false;
    }

    return wallet.participateAssetIssue(to, assertName.getBytes(), amount);
  }

  public boolean assetIssue(String name, long totalSupply, int trxNum, int icoNum, int precision,
      long startTime, long endTime, int voteScore, String description, String url,
      long freeNetLimit, long publicFreeNetLimit, HashMap<String, String> frozenSupply)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: assetIssue failed,  Please login first !!");
      return false;
    }

    Contract.AssetIssueContract.Builder builder = Contract.AssetIssueContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(wallet.getAddress()));
    builder.setName(ByteString.copyFrom(name.getBytes()));

    if (totalSupply <= 0) {
      return false;
    }
    builder.setTotalSupply(totalSupply);

    if (trxNum <= 0) {
      return false;
    }
    builder.setTrxNum(trxNum);

    if (icoNum <= 0) {
      return false;
    }
    builder.setNum(icoNum);

    if (precision < 0) {
      return false;
    }
    builder.setPrecision(precision);

    long now = System.currentTimeMillis();
    if (startTime <= now) {
      return false;
    }
    if (endTime <= startTime) {
      return false;
    }

    if (freeNetLimit < 0) {
      return false;
    }
    if (publicFreeNetLimit < 0) {
      return false;
    }

    builder.setStartTime(startTime);
    builder.setEndTime(endTime);
    builder.setVoteScore(voteScore);
    builder.setDescription(ByteString.copyFrom(description.getBytes()));
    builder.setUrl(ByteString.copyFrom(url.getBytes()));
    builder.setFreeAssetNetLimit(freeNetLimit);
    builder.setPublicFreeAssetNetLimit(publicFreeNetLimit);

    for (String daysStr : frozenSupply.keySet()) {
      String amountStr = frozenSupply.get(daysStr);
      long amount = Long.parseLong(amountStr);
      long days = Long.parseLong(daysStr);
      Contract.AssetIssueContract.FrozenSupply.Builder frozenSupplyBuilder
          = Contract.AssetIssueContract.FrozenSupply.newBuilder();
      frozenSupplyBuilder.setFrozenAmount(amount);
      frozenSupplyBuilder.setFrozenDays(days);
      builder.addFrozenSupply(frozenSupplyBuilder.build());
    }

    return wallet.createAssetIssue(builder.build());
  }

  public boolean createAccount(String address)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: createAccount failed,  Please login first !!");
      return false;
    }

    byte[] addressBytes = WalletApi.decodeFromBase58Check(address);
    return wallet.createAccount(addressBytes);
  }

  public AddressPrKeyPairMessage generateAddress() {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: createAccount failed,  Please login first !!");
      return null;
    }
    return WalletApi.generateAddress();
  }

  public boolean createWitness(String url) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: createWitness failed,  Please login first !!");
      return false;
    }

    return wallet.createWitness(url.getBytes());
  }

  public boolean updateWitness(String url) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: updateWitness failed,  Please login first !!");
      return false;
    }

    return wallet.updateWitness(url.getBytes());
  }

  public Block getBlock(long blockNum) {
    return WalletApi.getBlock(blockNum);
  }

  public long getTransactionCountByBlockNum(long blockNum) {
    return WalletApi.getTransactionCountByBlockNum(blockNum);
  }

  public BlockExtention getBlock2(long blockNum) {
    return WalletApi.getBlock2(blockNum);
  }

  public boolean voteWitness(HashMap<String, String> witness)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: VoteWitness failed,  Please login first !!");
      return false;
    }

    return wallet.voteWitness(witness);
  }

  public AssetIssueContract getAssetIssueByName(String assetName) {
    return WalletApi.getAssetIssueByName(assetName);
  }

  public AssetIssueContract getAssetIssueById(String assetId) {
    return WalletApi.getAssetIssueById(assetId);
  }

  public GrpcAPI.NumberMessage getTotalTransaction() {
    return WalletApi.getTotalTransaction();
  }

  public GrpcAPI.NumberMessage getNextMaintenanceTime() {
    return WalletApi.getNextMaintenanceTime();
  }

  public boolean updateAccount(byte[] accountNameBytes)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: updateAccount failed, Please login first !!");
      return false;
    }

    return wallet.updateAccount(accountNameBytes);
  }

  public boolean setAccountId(byte[] accountIdBytes)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: setAccount failed, Please login first !!");
      return false;
    }

    return wallet.setAccountId(accountIdBytes);
  }

  public boolean updateSetting(byte[] contractAddress, long consumeUserResourcePercent)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: updateSetting failed,  Please login first !!");
      return false;
    }
    return wallet.updateSetting(contractAddress, consumeUserResourcePercent);

  }

  public boolean updateEnergyLimit(byte[] contractAddress, long originEnergyLimit)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: updateSetting failed,  Please login first !!");
      return false;
    }
    return wallet.updateEnergyLimit(contractAddress, originEnergyLimit);

  }

  public boolean deployContract(String name, String abiStr, String codeStr,
      long feeLimit, long value, long consumeUserResourcePercent, long originEnergyLimit,
      long tokenValue, String tokenId, String libraryAddressPair)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: createContract failed,  Please login first !!");
      return false;
    }
    return wallet
        .deployContract(name, abiStr, codeStr, feeLimit, value, consumeUserResourcePercent,
            originEnergyLimit, tokenValue, tokenId,
            libraryAddressPair);
  }

  public boolean callContract(byte[] contractAddress, long callValue, byte[] data, long feeLimit,
      long tokenValue, String tokenId)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: callContract failed,  Please login first !!");
      return false;
    }

    return wallet.triggerContract(contractAddress, callValue, data, feeLimit, tokenValue, tokenId);
  }

  public Protocol.SmartContract getContract(String address) throws Exception {
    return wallet.getContract(WalletApi.decodeFromBase58Check(address));
  }

  public Protocol.SmartContract.ABI getContractABI(String address) throws Exception {
    return getContract(address).getAbi();
  }

  public String triggerContract(String walletAddress, String contractAddress, String functionName, String functionParams) {
    try {
      byte[] data = Hex.decode(AbiUtil.parseMethod(functionName, functionParams, false));
      byte[] owner = walletAddress.getBytes();
      byte[] contractAddressBytes = WalletApi.decodeFromBase58Check(contractAddress);

      Contract.TriggerSmartContract triggerSmartContract = Contract.TriggerSmartContract.newBuilder()
              .setOwnerAddress(ByteString.copyFrom(owner)).setContractAddress(ByteString.copyFrom(contractAddressBytes))
              .setData(ByteString.copyFrom(data)).setCallValue(0).build();
      GrpcAPI.TransactionExtention transactionExtention = WalletApi.rpcCli.triggerContract(triggerSmartContract);
      if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
        System.out.println("RPC create call trx failed!");
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
        return "";
      }

      Protocol.Transaction resultTransaction = transactionExtention.getTransaction();

      if (resultTransaction.getRetCount() != 0 &&
              transactionExtention.getConstantResult(0) != null &&
              transactionExtention.getResult() != null) {
        byte[] result = transactionExtention.getConstantResult(0).toByteArray();
        long balance = Hex2Decimal.hex2Decimal(org.spongycastle.util.encoders.Hex.toHexString(result));
        System.out.println(balance);
      }
      String transactionId = Hex.toHexString(Sha256Hash.of(transactionExtention.getTransaction().getRawData().toByteArray()).getBytes());
      return transactionId;


    } catch (Exception e) {
      e.printStackTrace();
    }

    return "";
  }

  public static GrpcAPI.TransactionExtention triggerContract(String[] parameters, byte[] ower)
          throws IOException, CipherException, CancelException, EncodingException {
    if (parameters == null ||
            parameters.length < 6) {
      System.out.println("TriggerContract needs 6 parameters like following: ");
      System.out.println(
              "TriggerContract contractAddress method args isHex fee_limit value");
      return null;
    }

    String contractAddrStr = parameters[0];
    String methodStr = parameters[1];
    String argsStr = parameters[2];
    boolean isHex = Boolean.valueOf(parameters[3]);
    long feeLimit = Long.valueOf(parameters[4]);
    long callValue = Long.valueOf(parameters[5]);
    if (argsStr.equalsIgnoreCase("#")) {
      argsStr = "";
    }

    byte[] input = Hex.decode(AbiUtil.parseMethod(methodStr, argsStr, isHex));
    byte[] contractAddress = StringTronUtil.decodeFromBase58Check(contractAddrStr);

    return triggerContract(ower, contractAddress, callValue, input, feeLimit);
  }

  public static Contract.TriggerSmartContract triggerCallContract(byte[] address,
                                                                  byte[] contractAddress,
                                                                  long callValue, byte[] data) {
    Contract.TriggerSmartContract.Builder builder = Contract.TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(address));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(data));
    builder.setCallValue(callValue);
    return builder.build();
  }

  public static GrpcAPI.TransactionExtention triggerContract(byte[] owner, byte[] contractAddress, long callValue, byte[] data, long feeLimit)
          throws IOException, CipherException, CancelException {
    Contract.TriggerSmartContract triggerContract = triggerCallContract(owner, contractAddress,
            callValue, data);

    GrpcAPI.TransactionExtention transactionExtention = WalletApi.rpcCli.triggerContract(triggerContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            System.out.println("RPC create call trx failed!");
            System.out.println("Code = " + transactionExtention.getResult().getCode());
            System.out
                    .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      return null;
    }

    Protocol.Transaction transaction = transactionExtention.getTransaction();
    if (transaction.getRetCount() != 0 &&
            transactionExtention.getConstantResult(0) != null &&
            transactionExtention.getResult() != null) {
      byte[] result = transactionExtention.getConstantResult(0).toByteArray();

      return transactionExtention;
    }

    GrpcAPI.TransactionExtention.Builder texBuilder = GrpcAPI.TransactionExtention.newBuilder();
    Protocol.Transaction.Builder transBuilder = Protocol.Transaction.newBuilder();
    Protocol.Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
            .toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Protocol.Transaction.Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();

    return transactionExtention;
  }

  public boolean processTransactionExtention(GrpcAPI.TransactionExtention transactionExtention) {
    GrpcAPI.Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    System.out.println(
            "Receive unsigned txid = " + ByteArray
                    .toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(transaction);
    return wallet.rpcCli.broadcastTransaction(transaction);
  }

  public Protocol.TransactionInfo getTransaction(String transactionId) {
    Protocol.TransactionInfo transactionInfo = WalletApi.rpcCli.getTransactionInfoById(transactionId).get();
    StringBuilder rawBuilder1 = new StringBuilder();
    transactionInfo.getContractResultList().forEach(result -> {
      rawBuilder1.append(Hex.toHexString(result.toByteArray()));
    });
    return transactionInfo;
  }

  public Protocol.Transaction signTransaction(Protocol.Transaction transaction) {
    if (transaction.getRawData().getTimestamp() == 0) {
      transaction = TransactionUtils.setTimestamp(transaction);
    }

    System.out.println(
            "Signed txid = " + ByteArray
                    .toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray())));
    transaction = TransactionUtils.sign(transaction, ecKey);
    return transaction;
  }

  public Protocol.Transaction signTransaction(Protocol.Transaction transaction, ECKey ecKey) {
    if (transaction.getRawData().getTimestamp() == 0) {
      transaction = TransactionUtils.setTimestamp(transaction);
    }

    System.out.println(
            "Signed txid = " + ByteArray
                    .toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray())));
    transaction = TransactionUtils.sign(transaction, ecKey);
    return transaction;
  }


}
