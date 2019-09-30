/*
 * Copyright © 2017-2019 WireGuard LLC.
 * Copyright © 2018-2019 Harsh Shandilya <msfjarvis@gmail.com>. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.wireguard.config

import android.net.InetAddresses
import android.net.TrafficStats
import android.os.Build
import org.xbill.DNS.Address
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Utility methods for creating instances of [InetAddress].
 */
object InetAddressUtils {
    private val PARSER_METHOD: Method by lazy { InetAddress::class.java.getMethod("parseNumericAddress", String::class.java) }
    private val IPV4_PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$".toRegex()

    /**
     * Parses an alphanumeric IPv4 or IPv6 address, performing DNS lookups when required.
     *
     * @param address a string representing the IP address
     * @return an instance of [Inet4Address] or [Inet6Address], as appropriate
     */
    @Throws(ParseException::class)
    fun parse(address: String): InetAddress {
        if (address.isEmpty())
            throw ParseException(InetAddress::class.java, address, "Empty address")
        try {
            return if (address.matches(IPV4_PATTERN)) {
                if (Build.VERSION.SDK_INT < 29) {
                    PARSER_METHOD.invoke(null, address) as InetAddress
                } else {
                    InetAddresses.parseNumericAddress(address)
                }
            } else {
                val executor = Executors.newSingleThreadExecutor()
                val future = executor.submit(Callable<InetAddress> {
                    TrafficStats.setThreadStatsTag(0)
                    Address.getByName(address)
                })
                executor.shutdown()
                future.get() as InetAddress
            }
        } catch (e: IllegalAccessException) {
            val cause = e.cause
            // Re-throw parsing exceptions with the original type, as callers might try to catch
            // them. On the other hand, callers cannot be expected to handle reflection failures.
            if (cause is IllegalArgumentException)
                throw ParseException(InetAddress::class.java, address, cause)
            throw RuntimeException(e)
        } catch (e: IllegalArgumentException) {
            throw ParseException(InetAddress::class.java, address, e)
        } catch (e: InvocationTargetException) {
            val cause = e.cause
            if (cause is IllegalArgumentException)
                throw ParseException(InetAddress::class.java, address, cause)
            throw RuntimeException(e)
        }
    }
}
