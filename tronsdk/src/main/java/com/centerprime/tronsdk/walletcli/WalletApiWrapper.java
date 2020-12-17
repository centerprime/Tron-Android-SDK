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
import com.centerprime.tronsdk.common.utils.TransactionUtils;
import com.centerprime.tronsdk.core.exception.CancelException;
import com.centerprime.tronsdk.core.exception.CipherException;
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

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class WalletApiWrapper {

  //private static final Logger logger = LoggerFactory.getLogger("WalletApiWrapper");
  public WalletApi wallet;
  public Context context;

  public ECKey ecKey = null;


  public String registerWallet(char[] password, Context context)
          throws CipherException, IOException
  {
    if (!WalletApi.passwordValid(password)) {
      return null;
    }

    byte[] passwd = StringUtils.char2Byte(password);

    wallet = new WalletApi(passwd);
    wallet.setLogin();

    String address = getAddress();

    StringUtils.clear(passwd);

    String keystoreName = wallet.store2Keystore("",context);

    return keystoreName;
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

    String keystoreName = wallet.store2Keystore("", context);
    //logout();
    return keystoreName;
  }

  public boolean changePassword(char[] oldPassword, char[] newPassword, String keystore, Context c)
      throws IOException, CipherException {
    logout();
    if (!WalletApi.passwordValid(newPassword)) {
      Log.d("tag", "Warning: ChangePassword failed, NewPassword is invalid !!");
      return false;
    }

    byte[] oldPasswd = StringUtils.char2Byte(oldPassword);
    byte[] newPasswd = StringUtils.char2Byte(newPassword);

    boolean result = WalletApi.changeKeystorePassword(oldPasswd, newPasswd, keystore, c);
    StringUtils.clear(oldPasswd);
    StringUtils.clear(newPasswd);

    return result;
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
    //Neddn't logout
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

  public Optional<WitnessList> listWitnesses() {
    try {
      return WalletApi.listWitnesses();
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public Optional<AssetIssueList> getAssetIssueList() {
    try {
      return WalletApi.getAssetIssueList();
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public Optional<AssetIssueList> getAssetIssueList(long offset, long limit) {
    try {
      return WalletApi.getAssetIssueList(offset, limit);
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public AssetIssueContract getAssetIssueByName(String assetName) {
    return WalletApi.getAssetIssueByName(assetName);
  }

  public Optional<AssetIssueList> getAssetIssueListByName(String assetName) {
    try {
      return WalletApi.getAssetIssueListByName(assetName);
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public AssetIssueContract getAssetIssueById(String assetId) {
    return WalletApi.getAssetIssueById(assetId);
  }

  public Optional<ProposalList> getProposalListPaginated(long offset, long limit) {
    try {
      return WalletApi.getProposalListPaginated(offset, limit);
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public Optional<ExchangeList> getExchangeListPaginated(long offset, long limit) {
    try {
      return WalletApi.getExchangeListPaginated(offset, limit);
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public Optional<NodeList> listNodes() {
    try {
      return WalletApi.listNodes();
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
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

  public boolean updateAsset(byte[] description, byte[] url, long newLimit,
      long newPublicLimit) throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: updateAsset failed, Please login first !!");
      return false;
    }

    return wallet.updateAsset(description, url, newLimit, newPublicLimit);
  }

  public boolean freezeBalance(long frozen_balance, long frozen_duration, int resourceCode,
      String receiverAddress)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: freezeBalance failed, Please login first !!");
      return false;
    }

    return wallet.freezeBalance(frozen_balance, frozen_duration, resourceCode, receiverAddress);
  }

  public boolean buyStorage(long quantity)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: buyStorage failed, Please login first !!");
      return false;
    }

    return wallet.buyStorage(quantity);
  }

  public boolean buyStorageBytes(long bytes)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: buyStorageBytes failed, Please login first !!");
      return false;
    }

    return wallet.buyStorageBytes(bytes);
  }

  public boolean sellStorage(long storageBytes)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: sellStorage failed, Please login first !!");
      return false;
    }

    return wallet.sellStorage(storageBytes);
  }

  public boolean unfreezeBalance(int resourceCode, String receiverAddress)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: unfreezeBalance failed, Please login first !!");
      return false;
    }

    return wallet.unfreezeBalance(resourceCode, receiverAddress);
  }

  public boolean unfreezeAsset() throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: unfreezeAsset failed, Please login first !!");
      return false;
    }

    return wallet.unfreezeAsset();
  }

  public boolean withdrawBalance() throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: withdrawBalance failed, Please login first !!");
      return false;
    }

    return wallet.withdrawBalance();
  }

  public boolean createProposal(HashMap<Long, Long> parametersMap)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: createProposal failed, Please login first !!");
      return false;
    }

    return wallet.createProposal(parametersMap);
  }

  public Optional<ProposalList> getProposalsList() {
    try {
      return WalletApi.listProposals();
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public Optional<Proposal> getProposals(String id) {
    try {
      return WalletApi.getProposal(id);
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public Optional<ExchangeList> getExchangeList() {
    try {
      return WalletApi.listExchanges();
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public Optional<Exchange> getExchange(String id) {
    try {
      return WalletApi.getExchange(id);
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public Optional<ChainParameters> getChainParameters() {
    try {
      return WalletApi.getChainParameters();
    } catch (Exception ex) {
      ex.printStackTrace();
      return Optional.empty();
    }
  }

  public boolean approveProposal(long id, boolean is_add_approval)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: approveProposal failed, Please login first !!");
      return false;
    }

    return wallet.approveProposal(id, is_add_approval);
  }

  public boolean deleteProposal(long id)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: deleteProposal failed, Please login first !!");
      return false;
    }

    return wallet.deleteProposal(id);
  }

  public boolean exchangeCreate(byte[] firstTokenId, long firstTokenBalance,
      byte[] secondTokenId, long secondTokenBalance)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: exchangeCreate failed, Please login first !!");
      return false;
    }

    return wallet.exchangeCreate(firstTokenId, firstTokenBalance,
        secondTokenId, secondTokenBalance);
  }

  public boolean exchangeInject(long exchangeId, byte[] tokenId, long quant)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: exchangeInject failed, Please login first !!");
      return false;
    }

    return wallet.exchangeInject(exchangeId, tokenId, quant);
  }

  public boolean exchangeWithdraw(long exchangeId, byte[] tokenId, long quant)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: exchangeWithdraw failed, Please login first !!");
      return false;
    }

    return wallet.exchangeWithdraw(exchangeId, tokenId, quant);
  }

  public boolean exchangeTransaction(long exchangeId, byte[] tokenId, long quant, long expected)
      throws CipherException, IOException, CancelException {
    if (wallet == null || !wallet.isLoginState()) {
      Log.d("tag", "Warning: exchangeTransaction failed, Please login first !!");
      return false;
    }

    return wallet.exchangeTransaction(exchangeId, tokenId, quant, expected);
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

  public String triggerContract(String contractAddress, String functionName, String functionParams) {
    try {
      Map<String, String> signedMethods = new HashMap<>();
      Map<String, Function> functionMap = new HashMap<>();
      Protocol.SmartContract.ABI abi = getContractABI(contractAddress);

      for (Protocol.SmartContract.ABI.Entry entry : abi.getEntrysList()) {
        String name = entry.getName();
        StringBuilder methodStr = new StringBuilder(name);
        methodStr.append("(");
        if (entry.getInputsList() != null && entry.getInputsList().size() > 0) {
          for (int j = 0; j < entry.getInputsList().size(); j++) {
            Protocol.SmartContract.ABI.Entry.Param param = entry.getInputsList().get(j);
            String inputName = param.getName();
            String type = param.getType();
            methodStr.append(type);
            if (j != entry.getInputsList().size() - 1) {
              methodStr.append(",");
            }
          }
        }
        methodStr.append(")");
        signedMethods.put(name, methodStr.toString());

        List<Protocol.SmartContract.ABI.Entry.Param> outputList = entry.getOutputsList();
        List<TypeReference<?>> outputParaList = new ArrayList<>();
        if (outputList != null && outputList.size() > 0) {
          for (int j = 0; j < outputList.size(); j++) {
            String type = outputList.get(j).getType();
            outputParaList.add(AbiTypes.getTypeReference(type, false));
          }
        }
        Function functionForReturn = new Function(entry.getName(),
                Collections.emptyList(), outputParaList);
        functionMap.put(name, functionForReturn);
      }

      byte[] data = Hex.decode(AbiUtil.parseMethod(signedMethods.get(functionName), functionParams, false));
      byte[] owner = wallet.getAddress();
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

      String transactionId;
      if (transactionExtention.getConstantResultCount() > 0) {
        byte[] result = transactionExtention.getConstantResult(0).toByteArray();
        System.out.println("message:" + transactionExtention.getTransaction().getRet(0).getRet());
        System.out.println(":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
        transactionId = Hex.toHexString(transactionExtention.getTxid().toByteArray());
        StringBuilder rawBuilder = new StringBuilder();
        transactionExtention.getConstantResultList().forEach(r -> {
          rawBuilder.append(Hex.toHexString(r.toByteArray()));
        });
        Function function = functionMap.get(functionName);
        List<com.centerprime.tronsdk.abi.datatypes.Type> output = null;
        StringBuilder outputBuilder = new StringBuilder();
        if (function != null) {
          output = FunctionReturnDecoder.decode(rawBuilder.toString(), function.getOutputParameters());
          for (int i = 0; i < output.size(); i++) {
            if (output.get(i) instanceof Address) {
              String hexTronAddress = ((Address) output.get(i)).toUint160().getValue()
                      .or(new BigInteger("410000000000000000000000000000000000000000", 16))
                      .toString(16);
              outputBuilder.append(WalletApi.encode58Check(Hex.decode(hexTronAddress)));
            } else {
              outputBuilder.append(output.get(i).getValue());
            }
            if (i != output.size() - 1) {
              outputBuilder.append(",");
            }
          }
        }
        System.out.println("transaction_id:" + transactionId);
        System.out.println("contract_result:" + (function == null ? rawBuilder.toString() : outputBuilder.toString()));
        return transactionId;
      }

      GrpcAPI.TransactionExtention.Builder texBuilder = GrpcAPI.TransactionExtention.newBuilder();
      Protocol.Transaction.Builder transBuilder = Protocol.Transaction.newBuilder();
      Protocol.Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
              .toBuilder();
      rawBuilder.setFeeLimit(100 * 1000000l);
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

      processTransactionExtention(transactionExtention);


      transactionId = Hex.toHexString(Sha256Hash.of(transactionExtention.getTransaction().getRawData().toByteArray()).getBytes());
      return transactionId;


    } catch (Exception e) {
      e.printStackTrace();
    }

    return "";
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
//        Function function = functionMap.get(functionName);
//        List<com.centerprime.tronsdk.abi.datatypes.Type> output = null;
//        StringBuilder outputBuilder = new StringBuilder();
//        if (function != null) {
//            output = FunctionReturnDecoder.decode(rawBuilder1.toString(), function.getOutputParameters());
//            for (int i = 0; i < output.size(); i++) {
//                if (output.get(i) instanceof Address) {
//                    String hexTronAddress = ((Address) output.get(i)).toUint160().getValue()
//                            .or(new BigInteger("410000000000000000000000000000000000000000", 16))
//                            .toString(16);
//                    outputBuilder.append(WalletApi.encode58Check(Hex.decode(hexTronAddress)));
//                } else {
//                    outputBuilder.append(output.get(i).getValue());
//                }
//                if (i != output.size() - 1) {
//                    outputBuilder.append(",");
//                }
//            }
//        }
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



}
