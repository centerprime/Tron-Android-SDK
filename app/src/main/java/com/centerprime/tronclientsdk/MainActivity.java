package com.centerprime.tronclientsdk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.centerprime.tronsdk.protos.Protocol;
import com.centerprime.tronsdk.sdk.TronWalletManager;
import com.centerprime.tronsdk.walletserver.WalletApi;

import java.math.BigDecimal;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//
//        getBalance();
//        getTrxBalance();
        sendTrc20Token();
    }

    private void getTrxBalance() {
        TronWalletManager tronWalletManager = TronWalletManager.getInstance();
        tronWalletManager.init(this);

//        tronWalletManager.getTrc20Balance("TWd4WrZ9wn84f5x1hZhL4DHvk738ns5jwb","TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t")
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(transactionExtention -> {
//                }, error -> {
//                    error.printStackTrace();
//                });


        tronWalletManager.getTokenTRX20Balance("TWd4WrZ9wn84f5x1hZhL4DHvk738ns5jwb","TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t",this)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(m->{
                    System.out.println(m);
                }, error->{
                   error.printStackTrace();
                });

    }

    private void getBalance() {
        TronWalletManager tronWalletManager = TronWalletManager.getInstance();
        tronWalletManager.init(this);
        tronWalletManager.createWallet("12345",this)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(re->{
                    System.out.println(re);
                    tronWalletManager.getBalanceTrx(re.getAddress(), this)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(dec->{
                                System.out.println(dec);
                            }, error->{
                                error.printStackTrace();
                            });
                }, error -> {
                   error.printStackTrace();
                });

    }

    private void importByKeystore(){
        TronWalletManager tronWalletManager = TronWalletManager.getInstance();
        tronWalletManager.init(this);
        tronWalletManager.getTokenTRX20Balance("TD6X5g1Yrw2QxFs1A3hk11r52GxQRLtdNB","TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t",this)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(test->{
                    System.out.println(test);
                }, error ->{
                    System.out.println(error);
                });
//        String asd = "{\"address\":\"TBFeHZtBny2ywJ7CEh944fwuu22oGjNtsn\",\"id\":\"5cd1c011-46cf-4c6f-9f29-0b8a31e0cb4d\",\"version\":3,\"crypto\":{\"cipher\":\"aes-128-ctr\",\"cipherparams\":{\"iv\":\"291f3342eee30f918c0155945ff2c189\"},\"ciphertext\":\"bcb527b7922553489bfa5ed685be5243e38b5cd27999cb8e2ee815ad539e9560\",\"kdf\":\"scrypt\",\"kdfparams\":{\"dklen\":32,\"n\":262144,\"p\":1,\"r\":8,\"salt\":\"663ecd3d3a46e87ea3503cc4c17cd3ba60e6aa5afc8f50ea2b56964959b051bb\"},\"mac\":\"6557af65fe676a2ee8c336e0583530796cbce8c7be751e706e06d359ce84b1f7\"}}";
//        tronWalletManager.importByKeystore("test12434334",asd,this)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(test->{
//                    System.out.println(test);
//                }, error ->{
//                    System.out.println(error);
//                });
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

    private void importByPrivateKey(){

//        TronWalletManager tronWalletManager = TronWalletManager.getInstance();
//        tronWalletManager.importByPrivateKey(privateKey,"12345",this)
//        .subscribeOn(Schedulers.io())
//        .observeOn(AndroidSchedulers.mainThread())
//        .subscribe(wallet -> {
//            System.out.println(wallet);
//        }, error->{
//            error.printStackTrace();
//        });


    }


    private void sendTrc20Token(){
        TronWalletManager tronWalletManager = TronWalletManager.getInstance();
        tronWalletManager.init(this);

        tronWalletManager.createWallet("12345",this)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(wallet -> {
                });

//        tronWalletManager.sentTRX20(this,"TQjv4nR1Ezr7dKVyQALQvE5PpcwyRHCvyC","12345","TWfutLiMn46exyG9bxhtST4QGw4nxMSmop","TKFVm3segGn4bQjdGiiEMvXU73njx4K8go",new BigDecimal(10))
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(result ->{
//                    System.out.println(result);
//                }, error ->{
//                   error.printStackTrace();
//                });


//        tronWalletManager.getTrc20Balance("TQjv4nR1Ezr7dKVyQALQvE5PpcwyRHCvyC","TKFVm3segGn4bQjdGiiEMvXU73njx4K8go")
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(transactionExtention ->  {
//                }, error->{
//                    error.printStackTrace();
//                });


    }




}