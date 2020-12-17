package com.centerprime.tronsdk.abi.datatypes.generated;

import com.centerprime.tronsdk.abi.datatypes.Int;

import java.math.BigInteger;

/**
 * Auto generated code.
 * <p><strong>Do not modifiy!</strong>
 * <p>Please use org.web3j.codegen.AbiTypesGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 */
public class Int112 extends Int {
    public static final Int112 DEFAULT = new Int112(BigInteger.ZERO);

    public Int112(BigInteger value) {
        super(112, value);
    }

    public Int112(long value) {
        this(BigInteger.valueOf(value));
    }
}
