/*
 * Copyright 2004-2009 the Seasar Foundation and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.seasar.doma.wrapper;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;

import org.seasar.doma.DomaNullPointerException;

/**
 * {@link BigInteger} のラッパーです。
 * 
 * @author taedium
 * 
 */
public class BigIntegerWrapper extends AbstractWrapper<BigInteger> implements
        NumberWrapper<BigInteger>, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * インスタンスを構築します。
     */
    public BigIntegerWrapper() {
    }

    /**
     * 値を指定してインスタンスを構築します。
     * 
     * @param value
     *            値
     */
    public BigIntegerWrapper(BigInteger value) {
        super(value);
    }

    @Override
    public BigIntegerWrapper copy() {
        return new BigIntegerWrapper(value);
    }

    @Override
    public void set(Number v) {
        super.set(BigInteger.valueOf(v.longValue()));
    }

    @Override
    public <R, P, TH extends Throwable> R accept(
            WrapperVisitor<R, P, TH> visitor, P p) throws TH {
        if (visitor == null) {
            throw new DomaNullPointerException("visitor");
        }
        if (BigIntegerWrapperVisitor.class.isInstance(visitor)) {
            @SuppressWarnings("unchecked")
            BigIntegerWrapperVisitor<R, P, TH> v = (BigIntegerWrapperVisitor) visitor;
            return v.visitBigIntegerWrapper(this, p);
        }
        return visitor.visitUnknownWrapper(this, p);
    }

    private void readObject(ObjectInputStream inputStream) throws IOException,
            ClassNotFoundException {
        inputStream.defaultReadObject();
        value = (BigInteger) inputStream.readObject();
    }

    private void writeObject(ObjectOutputStream outputStream)
            throws IOException {
        outputStream.defaultWriteObject();
        outputStream.writeObject(value);
    }
}
