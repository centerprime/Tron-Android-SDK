package com.centerprime.tronsdk.sdk;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import com.centerprime.tronsdk.api.GrpcAPI;
import com.centerprime.tronsdk.common.utils.ByteArray;
import com.centerprime.tronsdk.core.exception.CipherException;
import com.centerprime.tronsdk.keystore.StringUtils;
import com.centerprime.tronsdk.keystore.WalletFile;
import com.centerprime.tronsdk.keystore.WalletUtils;
import com.centerprime.tronsdk.protos.Protocol;
import com.centerprime.tronsdk.walletcli.WalletApiWrapper;
import com.centerprime.tronsdk.walletserver.WalletApi;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by CenterPrime on 2020/12/14.
 */
public class TronWalletManager {

    private static final TronWalletManager ourInstance = new TronWalletManager();

    public static TronWalletManager getInstance() {
        return ourInstance;
    }

    public TronWalletManager() {
    }


    private TronApi tronApi;

    private HyperLedgerApi hyperLedgerApi;


    public void init(Context context) {
        WalletApi.reloadClient(context);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://apilist.tronscan.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
        tronApi = retrofit.create(TronApi.class);

        Retrofit hpRetrofit = new Retrofit.Builder()
                .baseUrl("http://34.231.96.72:8081")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
        hyperLedgerApi = hpRetrofit.create(HyperLedgerApi.class);
    }

    /**
     * Create Wallet by password
     */
    public Single<Wallet> createWallet(String password, Context context) {
        return Single.fromCallable(() -> {
            HashMap<String, Object> body = new HashMap<>();
            try {
                WalletApiWrapper walletApi = new WalletApiWrapper();
                walletApi.registerWallet(password.toCharArray(), context);
                String walletAddress = walletApi.getAddress();
                String keystore = getKeyStore(walletAddress, context).blockingGet();
                body.put("action_type", "WALLET_CREATE");
                body.put("wallet_address", walletAddress);
                body.put("status", "SUCCESS");
                sendEventToLedger(body, context);
                return new Wallet(walletAddress, keystore);
            } catch (Exception e) {
                e.printStackTrace();
                body.put("status", "FAILURE");
            }
            sendEventToLedger(body, context);
            return null;
        });
    }

    public Single<Wallet> importByKeystore(String password, String keystore, Context context) {
        return Single.fromCallable(() -> {
            HashMap<String, Object> body = new HashMap<>();
            try {
                WalletFile walletFile = WalletUtils.loadFromKeystore(keystore, context);
                WalletApi walletApi = new WalletApi(walletFile);
                byte[] passwd = StringUtils.char2Byte(password.toCharArray());
                boolean isSuccess = walletApi.checkPassword(passwd);
                body.put("action_type", "WALLET_IMPORT_KEYSTORE");
                body.put("wallet_address", walletFile.getAddress());
                body.put("status", "SUCCESS");
                sendEventToLedger(body, context);
                return new Wallet(walletFile.getAddress(), keystore);
            } catch (JsonSyntaxException | CipherException e) {
                e.printStackTrace();
                body.put("status", "FAILURE");
                sendEventToLedger(body, context);
                throw e;
            }
        });
    }

    /**
     * Get Trx Balance of Wallet
     */
    public Single<BigDecimal> getBalanceTrx(String address, Context context) {
        return Single.fromCallable(() -> {
            try {
                HashMap<String, Object> trx20Balance = tronApi.getTrxBalance(address).blockingGet();
                if (trx20Balance != null) {
                    double amount = (double) trx20Balance.get("balance");
                    BigDecimal big = new BigDecimal(amount);
                    big = big.divide(new BigDecimal(1000000));
                    double balance = big.floatValue();

                    HashMap<String, Object> body = new HashMap<>();
                    body.put("action_type", "COIN_BALANCE");
                    body.put("wallet_address", address);
                    body.put("balance", balance);
                    sendEventToLedger(body, context);

                    return new BigDecimal(balance);
                }

            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            return null;

        });
    }


    /**
     * Get TRX20 Token Balance of Wallet
     */
    public Single<BigDecimal> getTokenTRX20Balance(String walletAddress, String tokenContractAddress, Context context) {
        return Single.fromCallable(() -> {
            try {
                BigDecimal tokenBalance;
                HashMap<String, Object> trx20Balance = tronApi.getTrx20Balance(walletAddress, tokenContractAddress).blockingGet();
                if (trx20Balance.containsKey("tokens")) {
                    List<LinkedTreeMap<String, Object>> mapList = (List<LinkedTreeMap<String, Object>>) trx20Balance.get("tokens");
                    BigDecimal balance = BigDecimal.ZERO;
                    for (LinkedTreeMap<String, Object> map : mapList) {
                        if (map.get("tokenId").equals(tokenContractAddress)) {
                            double amount = Double.parseDouble((String) map.get("balance"));
                            balance = BigDecimal.valueOf(amount);
                        }
                    }
                    tokenBalance = balance;
                } else {
                    tokenBalance = BigDecimal.ZERO;
                }

                HashMap<String, Object> body = new HashMap<>();
                body.put("action_type", "TOKEN_BALANCE");
                body.put("wallet_address", walletAddress);
                body.put("balance", tokenBalance.doubleValue());
                sendEventToLedger(body, context);

                return tokenBalance;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            return null;

        });
    }


    public Single<String> sendTRX(Context context, String sender, String password, String receiver, BigDecimal amount) {
        return Single.fromCallable(() -> {
            BigDecimal big2 = amount;
            big2 = big2.multiply(new BigDecimal(1000000));
            String toAddress = receiver;

            WalletApiWrapper walletApi = new WalletApiWrapper();
            String walletPath = "tron_" + sender.toLowerCase() + ".json";
            walletApi.loginAndroid(password.toCharArray(), walletPath, context);
            byte[] priKey = walletApi.backupWallet(password.toCharArray(), walletPath, context);

            String privatekey = (bytesToHex(priKey));
            GrpcAPI.EasyTransferResponse response = WalletApi
                    .easyTransferByPrivate(ByteArray.fromHexString(privatekey),
                            WalletApi.decodeFromBase58Check(toAddress), big2.longValue());

            if (response.getResult().getResult() == true) {
                Protocol.Transaction transaction = response.getTransaction();
                System.out.println("Easy transfer successful!!!");
                String txId = ByteArray.toHexString(response.getTxid().toByteArray());
                System.out.println("Receive txid = " + txId);

                HashMap<String, Object> body = new HashMap<>();
                body.put("action_type", "SEND_TRX");
                body.put("from_wallet_address", sender);
                body.put("to_wallet_address", receiver);
                body.put("amount", amount.doubleValue());
                body.put("tx_hash", txId);
//                body.put("gasLimit", gasLimit.toString());
//                body.put("gasPrice", gasPrice.toString());
//                body.put("fee", gasLimit.multiply(gasPrice).toString());
                body.put("status", "SUCCESS");
                sendEventToLedger(body, context);

                return txId;
            }
            return "";
        });
    }

//    public Single<String> sentTRX20(Context context, String sender, String password, String receiver, String contractAddress, BigDecimal amount) {
//        return Single.fromCallable(() -> {
//
//            return null;
//        });
//    }


    public static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    /**
     * Get Keystore by wallet address
     */
    public Single<String> getKeyStore(String walletAddress, Context context) {
        return Single.fromCallable(() -> {

            String walletPath = context.getFilesDir().getPath() + "/" + "tron_" + walletAddress.toLowerCase() + ".json";
            File keystoreFile = new File(walletPath);
            if (keystoreFile.exists()) {
                return read_file(context, keystoreFile.getName());
            } else {
                throw new Exception("Keystore is NULL");
            }
        });
    }


    public String read_file(Context context, String filename) throws IOException {
        FileInputStream fis = context.openFileInput(filename);
        InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
        BufferedReader bufferedReader = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }


    private void sendEventToLedger(HashMap<String, Object> map, Context context) {
        try {
            SubmitTransactionModel submitTransactionModel = new SubmitTransactionModel();
            submitTransactionModel.setTx_type("TRON");
            submitTransactionModel.setUsername("user1");
            submitTransactionModel.setOrgname("org1");

            HashMap<String, Object> deviceInfo = deviceInfo(context);
            if (deviceInfo != null) {
                map.put("DEVICE_INFO", new Gson().toJson(deviceInfo));
            }

            submitTransactionModel.setBody(map);
            hyperLedgerApi.submitTransaction(submitTransactionModel)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((objectBaseResponse, throwable) -> {
                        System.out.println(objectBaseResponse);
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private HashMap<String, Object> deviceInfo(Context context) {
        try {
            String androidId = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            String osName = "ANDROID";
            String serialNumber = Build.SERIAL;
            String model = Build.MODEL;
            String manufacturer = Build.MANUFACTURER;
            HashMap<String, Object> deviceInfo = new HashMap<>();
            deviceInfo.put("ID", androidId);
            deviceInfo.put("OS", osName);
            deviceInfo.put("MODEL", model);
            deviceInfo.put("SERIAL", serialNumber);
            deviceInfo.put("MANUFACTURER", manufacturer);
            return deviceInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
