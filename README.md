# Tron Android SDK


## Table of Contents

- [Getting Started](#getting-started)
  - [Adding Tron Client SDK as a Maven Dependency](#adding-quark-chain-client-sdk-as-a-maven-dependency)
  - [Basic Usage](#basic-usage)
- [Features at a Glance](#features-at-a-glance)
  - [Create Wallet](#create-wallet)
  - [Import Wallet By Keystore](#import-wallet-by-keystore)
  - [Export Keystore](#export-keystore)
  - [TRX Balance](#trx-balance)
  - [TRC20 Balance](#trc20-balance)
  - [Send TRX](#send-trx)
  - [Send TRC20 Token](#send-trc20-token)

## Getting Started

### Add jitpack to your root gradle file at the end of repositories:
```xml
allprojects {
    repositories {
	...
        maven { url 'https://jitpack.io'}
    }
}
```

### Adding Tron Client SDK as a Maven Dependency

Maven:

```xml
<dependency>
	    <groupId>com.github.centerprime</groupId>
	    <artifactId>Tron-Client-SDK</artifactId>
	    <version>1.0.7</version>
</dependency>
```

Gradle:

```groovy
dependencies {
      implementation 'com.github.centerprime:Tron-Client-SDK:1.0.7'


}
```

### Basic Usage

Once you have the dependencies set up you can start using *CenterPrime* by creating a `Tron Wallet`:

```java

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TronWalletManager tronWalletManager = TronWalletManager.getInstance();
        tronWalletManager.init(this);

        tronWalletManager.createWallet("12345", this)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(wallet -> {
                    String walletAddress = wallet.getAddress();
                    String keystore = wallet.getKeystore();

                }, error -> {
                    System.out.println(error);
                });
    }
}
```

*Congratulations!* Now you are a *CenterPrime* user.

## Features at a Glance

### Create Wallet

> You can create Tron Wallet.
```java
TronWalletManager tronWalletManager = TronWalletManager.getInstance();
tronWalletManager.init(this);
tronWalletManager.createWallet("12345", this)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(wallet -> {
                    String walletAddress = wallet.getAddress();
                    String keystore = wallet.getKeystore();
		    
                }, error -> {
                    System.out.println(error);
                });

```

### Import Wallet By Keystore

> Import Tron Wallet by Keystore.

```java
TronWalletManager tronWalletManager = TronWalletManager.getInstance();
tronWalletManager.init(this);
String password = "xxxx12345";
String keystore = "JSON_FORMAT";
tronWalletManager.importFromKeystore(password, keystore, this)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(walletAddress -> {

                    Toast.makeText(this, "Wallet Address : " + walletAddress, Toast.LENGTH_SHORT).show();

                }, error -> {

                });
```
### Export Keystore

> If you want to export wallet address’s keystore you can use a code written below.

```java
TronWalletManager tronWalletManager = TronWalletManager.getInstance();
String walletAddress = "WALLET_ADDRESS";
tronWalletManager.getKeyStore(walletAddress, this)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(keystore -> {

                    Toast.makeText(this, "Keystore : " + keystore, Toast.LENGTH_SHORT).show();

                }, error -> {

                });
```

### TRX Balance

> TRX Balance.

```java
TronWalletManager tronWalletManager = TronWalletManager.getInstance();
tronWalletManager.init(this);
String walletAddress = "WALLET_ADDRESS";
tronWalletManager.getTRXBalance(walletAddress,this)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(balance -> {

                    Toast.makeText(this, "TRX Balance : " + balance, Toast.LENGTH_SHORT).show();

                }, error -> {

                });
```


### TRC20 Balance

> TRC20 Balance.

```java
TronWalletManager tronWalletManager = TronWalletManager.getInstance();
tronWalletManager.init(this);
String walletAddress = "WALLET_ADDRESS";
String tokrnContractAddress = "TOKEN_CONTRACT_ADDRESS";
tronWalletManager.getTokenTRX20Balance(walletAddress,tokrnContractAddress,this)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(balance -> {

                    Toast.makeText(this, "TRX Balance : " + balance, Toast.LENGTH_SHORT).show();

                }, error -> {

                });
```



### Send TRX

> Send TRX.

```java
TronWalletManager tronWalletManager = TronWalletManager.getInstance();
tronWalletManager.init(this);

BigDecimal trxAmount = new BigDecimal("TRX_AMOUNT");
String sender = "SENDER_WALLET_ADDRESS";
String password = "SENDER_WALLET_PASSWORD";
String receiver = "RECEIVER_WALLET_ADDRESS";
qkcManager.sendTRX(Context context, String sender, String password, String receiver, BigDecimal trxAmount)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(tx -> {

                    Toast.makeText(this, "TX : " + tx, Toast.LENGTH_SHORT).show();

                }, error -> {

                });
```
### Send TRC20 Token

> Send TRC20 Token.

```java
TronWalletManager tronWalletManager = TronWalletManager.getInstance();
        tronWalletManager.init(this);
        String sender = "SENDER_WALLET_ADDRESS";
        String sender_password = "SENDER_WALLET_PASSWORD";
        String receiver = "RECEIVER_WALLET_ADDRESS";
        String contractAddress = "TOKEN_CONTRACT_ADDRESS";
        BigDecimal tokenAmount = new BigDecimal("");
        tronWalletManager.sentTRX20(sender,
                sender_password, receiver,
                contractAddress,
                tokenAmount, this)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    System.out.println(result);
                }, error -> {
                    error.printStackTrace();
                });
```
