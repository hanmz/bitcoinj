/*
 * Copyright 2014 The bitcoinj authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoinj.wallet;

import org.bitcoinj.base.ScriptType;
import org.bitcoinj.crypto.AesKey;
import org.bitcoinj.crypto.ECKey;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A DecryptingKeyBag filters a pre-existing key bag, decrypting keys as they are requested using the provided
 * AES key. If the keys are encrypted and no AES key provided, {@link ECKey.KeyIsEncryptedException}
 * will be thrown.
 */
public class DecryptingKeyBag implements KeyBag {
    protected final KeyBag target;
    protected final AesKey aesKey;

    public DecryptingKeyBag(KeyBag target, @Nullable AesKey aesKey) {
        this.target = Objects.requireNonNull(target);
        this.aesKey = aesKey;
    }

    @Nullable
    private ECKey maybeDecrypt(ECKey key) {
        if (key == null)
            return null;
        else if (key.isEncrypted()) {
            if (aesKey == null)
                throw new ECKey.KeyIsEncryptedException();
            return key.decrypt(aesKey);
        } else {
            return key;
        }
    }

    private RedeemData maybeDecrypt(RedeemData redeemData) {
        List<ECKey> decryptedKeys = new ArrayList<>();
        for (ECKey key : redeemData.keys) {
            decryptedKeys.add(maybeDecrypt(key));
        }
        return RedeemData.of(decryptedKeys, redeemData.redeemScript);
    }

    @Nullable
    @Override
    public ECKey findKeyFromPubKeyHash(byte[] pubKeyHash, @Nullable ScriptType scriptType) {
        return maybeDecrypt(target.findKeyFromPubKeyHash(pubKeyHash, scriptType));
    }

    @Nullable
    @Override
    public ECKey findKeyFromPubKey(byte[] pubKey) {
        return maybeDecrypt(target.findKeyFromPubKey(pubKey));
    }

    @Nullable
    @Override
    public RedeemData findRedeemDataFromScriptHash(byte[] scriptHash) {
        return maybeDecrypt(target.findRedeemDataFromScriptHash(scriptHash));
    }
}
