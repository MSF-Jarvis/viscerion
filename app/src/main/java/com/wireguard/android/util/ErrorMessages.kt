/*
 * Copyright © 2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.util

import android.content.res.Resources

import com.wireguard.android.Application
import com.wireguard.android.R
import com.wireguard.config.BadConfigException
import com.wireguard.config.BadConfigException.Reason
import com.wireguard.config.InetEndpoint
import com.wireguard.config.InetNetwork
import com.wireguard.crypto.ParseException
import com.wireguard.crypto.Key.Format
import com.wireguard.crypto.KeyFormatException
import com.wireguard.crypto.KeyFormatException.Type
import java9.util.Maps

import java.net.InetAddress
import java.util.EnumMap

import com.sun.jmx.remote.util.EnvHelp.getCause
import jdk.nashorn.internal.runtime.ECMAErrors.getMessage

object ErrorMessages {
    private val BCE_REASON_MAP = EnumMap<Reason, Int>(
        Maps.of<K, V>(
            Reason.INVALID_KEY, R.string.bad_config_reason_invalid_key,
            Reason.INVALID_NUMBER, R.string.bad_config_reason_invalid_number,
            Reason.INVALID_VALUE, R.string.bad_config_reason_invalid_value,
            Reason.MISSING_ATTRIBUTE, R.string.bad_config_reason_missing_attribute,
            Reason.MISSING_SECTION, R.string.bad_config_reason_missing_section,
            Reason.MISSING_VALUE, R.string.bad_config_reason_missing_value,
            Reason.SYNTAX_ERROR, R.string.bad_config_reason_syntax_error,
            Reason.UNKNOWN_ATTRIBUTE, R.string.bad_config_reason_unknown_attribute,
            Reason.UNKNOWN_SECTION, R.string.bad_config_reason_unknown_section
        )
    )
    private val KFE_FORMAT_MAP = EnumMap<Format, Int>(
        Maps.of<K, V>(
            Format.BASE64, R.string.key_length_explanation_base64,
            Format.BINARY, R.string.key_length_explanation_binary,
            Format.HEX, R.string.key_length_explanation_hex
        )
    )
    private val KFE_TYPE_MAP = EnumMap<Type, Int>(
        Maps.of<K, V>(
            Type.CONTENTS, R.string.key_contents_error,
            Type.LENGTH, R.string.key_length_error
        )
    )
    private val PE_CLASS_MAP = Maps.of<Class<*>, Int>(
        InetAddress::class.java, R.string.parse_error_inet_address,
        InetEndpoint::class.java, R.string.parse_error_inet_endpoint,
        InetNetwork::class.java, R.string.parse_error_inet_network,
        Int::class.java, R.string.parse_error_integer
    )

    operator fun get(@Nullable throwable: Throwable?): String {
        val resources = Application.get().resources
        if (throwable == null)
            return resources.getString(R.string.unknown_error)
        val rootCause = rootCause(throwable)
        val message: String
        if (rootCause is BadConfigException) {
            val bce = rootCause as BadConfigException
            val reason = getBadConfigExceptionReason(resources, bce)
            val context = if (bce.getLocation() === Location.TOP_LEVEL)
                resources.getString(
                    R.string.bad_config_context_top_level,
                    bce.getSection().getName()
                )
            else
                resources.getString(
                    R.string.bad_config_context,
                    bce.getSection().getName(),
                    bce.getLocation().getName()
                )
            val explanation = getBadConfigExceptionExplanation(resources, bce)
            message = resources.getString(R.string.bad_config_error, reason, context) + explanation
        } else if (rootCause.message != null) {
            message = rootCause.message
        } else {
            val errorType = rootCause.javaClass.simpleName
            message = resources.getString(R.string.generic_error, errorType)
        }
        return message
    }

    private fun getBadConfigExceptionExplanation(
        resources: Resources,
        bce: BadConfigException
    ): String {
        if (bce.getCause() is KeyFormatException) {
            val kfe = bce.getCause() as KeyFormatException
            if (kfe.getType() === Type.LENGTH)
                return resources.getString(KFE_FORMAT_MAP[kfe.getFormat()]!!)
        } else if (bce.getCause() is ParseException) {
            val pe = bce.getCause() as ParseException
            if (pe.getMessage() != null)
                return ": " + pe.getMessage()
        } else if (bce.getLocation() === Location.LISTEN_PORT) {
            return resources.getString(R.string.bad_config_explanation_udp_port)
        } else if (bce.getLocation() === Location.MTU) {
            return resources.getString(R.string.bad_config_explanation_positive_number)
        } else if (bce.getLocation() === Location.PERSISTENT_KEEPALIVE) {
            return resources.getString(R.string.bad_config_explanation_pka)
        }
        return ""
    }

    private fun getBadConfigExceptionReason(
        resources: Resources,
        bce: BadConfigException
    ): String {
        if (bce.getCause() is KeyFormatException) {
            val kfe = bce.getCause() as KeyFormatException
            return resources.getString(KFE_TYPE_MAP[kfe.getType()]!!)
        } else if (bce.getCause() is ParseException) {
            val pe = bce.getCause() as ParseException
            val type = resources.getString(
                if (PE_CLASS_MAP.containsKey(pe.getParsingClass()))
                    PE_CLASS_MAP[pe.getParsingClass()]
                else
                    R.string.parse_error_generic
            )
            return resources.getString(R.string.parse_error_reason, type, pe.getText())
        }
        return resources.getString(BCE_REASON_MAP[bce.getReason()], bce.getText())
    }

    private fun rootCause(throwable: Throwable): Throwable {
        var cause = throwable
        while (cause.cause != null) {
            if (cause is BadConfigException)
                break
            cause = cause.cause
        }
        return cause
    }
} // Prevent instantiation