package com.centerprime.tronclientsdk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.centerprime.tronsdk.sdk.TronWalletManager;

import java.math.BigDecimal;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        getTrxBalance();
    }

    private void getTrxBalance() {
//        Completable.fromAction(() -> {
////            TronAPI.decodeFromBase58Check(addr)
//            Protocol.Account account = TronAPI.queryAccount("TDexXn5onbVCgCatEnKKgbDsFY6fC3E7sU".getBytes(), true);
//            System.out.println(account);
//        }).subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe();

//        registerWallet("test12434334");


//        getBalance();
//
        importByKeystore();
    }

    private void getBalance() {


    }

    private void importByKeystore(){
        TronWalletManager tronWalletManager = TronWalletManager.getInstance();
        tronWalletManager.init(this);

        String asd = "{\"address\":\"TBFeHZtBny2ywJ7CEh944fwuu22oGjNtsn\",\"id\":\"5cd1c011-46cf-4c6f-9f29-0b8a31e0cb4d\",\"version\":3,\"crypto\":{\"cipher\":\"aes-128-ctr\",\"cipherparams\":{\"iv\":\"291f3342eee30f918c0155945ff2c189\"},\"ciphertext\":\"bcb527b7922553489bfa5ed685be5243e38b5cd27999cb8e2ee815ad539e9560\",\"kdf\":\"scrypt\",\"kdfparams\":{\"dklen\":32,\"n\":262144,\"p\":1,\"r\":8,\"salt\":\"663ecd3d3a46e87ea3503cc4c17cd3ba60e6aa5afc8f50ea2b56964959b051bb\"},\"mac\":\"6557af65fe676a2ee8c336e0583530796cbce8c7be751e706e06d359ce84b1f7\"}}";
        tronWalletManager.importByKeystore("test12434334",asd,this)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(test->{
                    System.out.println(test);
                }, error ->{
                    System.out.println(error);
                });
    }

    public void registerWallet(String keyWords)
    {

        TronWalletManager tronWalletManager = TronWalletManager.getInstance();
        tronWalletManager.init(this);

        tronWalletManager.sendTRX(this, "TBFeHZtBny2ywJ7CEh944fwuu22oGjNtsn", keyWords, "THQJBkJc36mN6MSE8DXCzcQ5ruWwM216Xv", new BigDecimal(2))
                .observeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(test -> {
                    System.out.println(test);
                }, error -> {
                    System.out.println(error);
                });



    }

    public void create() {
//        String name = "TronLink";
//        String password = "12345678";
//        try {
//            Wallet wallet = new Wallet(true);
//            if (wallet != null) {
//                wallet.setWalletName(name);
//                wallet.setCreateTime(System.currentTimeMillis());
//                WalletUtils.saveWallet(wallet, password);
//
//                byte[] pr = wallet.getPrivateKey();
//
//                String address = wallet.getAddress();
//                wallet.getECKey().getPubKey();
//
//                System.out.println(address);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
}