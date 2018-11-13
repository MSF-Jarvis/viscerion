/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.crypto

import java.security.SecureRandom
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * Represents a Curve25519 keypair as used by WireGuard.
 */

class Keypair private constructor(val privateKey: ByteArray) {
    val publicKey: ByteArray

    init {
        publicKey = generatePublicKey(privateKey)
    }

    constructor() : this(generatePrivateKey())

    constructor(privateKey: String) : this(KeyEncoding.keyFromBase64(privateKey))

    fun getPrivateKey(): String {
        return KeyEncoding.keyToBase64(privateKey)
    }

    fun getPublicKey(): String {
        return KeyEncoding.keyToBase64(publicKey)
    }

    private companion object {
        private fun generatePrivateKey(): ByteArray {
            val secureRandom = SecureRandom()
            val privateKey = ByteArray(KeyEncoding.KEY_LENGTH)
            secureRandom.nextBytes(privateKey)
            privateKey[0] = privateKey[0] and 248.toByte()
            privateKey[31] = privateKey[31] and 127
            privateKey[31] = privateKey[31] or 64
            return privateKey
        }

        private fun generatePublicKey(privateKey: ByteArray): ByteArray {
            val publicKey = ByteArray(KeyEncoding.KEY_LENGTH)
            Curve25519.eval(publicKey, 0, privateKey, null)
            return publicKey
        }
    }
}
