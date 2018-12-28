/*
 * Copyright © 2017-2018 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.viewmodel

import android.os.Parcel
import android.os.Parcelable
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.ObservableArrayList
import androidx.databinding.ObservableList

import com.wireguard.android.BR
import com.wireguard.config.Attribute
import com.wireguard.config.BadConfigException
import com.wireguard.config.Interface
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyFormatException
import com.wireguard.crypto.KeyPair

import java9.util.stream.Collectors
import java9.util.stream.StreamSupport
import java.net.InetAddress

class InterfaceProxy : BaseObservable, Parcelable {

    private val excludedApplications: ObservableList<String> = ObservableArrayList()
    private var addresses: String? = null
    private var dnsServers: String? = null
    private var listenPort: String? = null
    private var mtu: String? = null
    private var privateKey: String? = null
    var publicKey: String? = null
        private set

    private constructor(`in`: Parcel) {
        addresses = `in`.readString()
        dnsServers = `in`.readString()
        `in`.readStringList(excludedApplications)
        listenPort = `in`.readString()
        mtu = `in`.readString()
        privateKey = `in`.readString()
        publicKey = `in`.readString()
    }

    constructor(other: Interface) {
        addresses = Attribute.join(other.addresses)
        val dnsServerStrings = StreamSupport.stream(other.dnsServers)
            .map(InetAddress::getHostAddress)
            .collect(Collectors.toUnmodifiableList<Any>())
        dnsServers = Attribute.join(dnsServerStrings)
        excludedApplications.addAll(other.excludedApplications)
        listenPort = other.listenPort?.toString() ?: ""
        mtu = other.mtu?.toString() ?: ""
        val keyPair = other.keyPair
        privateKey = keyPair.privateKey.toBase64()
        publicKey = keyPair.publicKey.toBase64()
    }

    constructor() {
        addresses = ""
        dnsServers = ""
        listenPort = ""
        mtu = ""
        privateKey = ""
        publicKey = ""
    }

    override fun describeContents(): Int {
        return 0
    }

    fun generateKeyPair() {
        val keyPair = KeyPair()
        privateKey = keyPair.privateKey.toBase64()
        publicKey = keyPair.publicKey.toBase64()
        notifyPropertyChanged(BR.privateKey)
        notifyPropertyChanged(BR.publicKey)
    }

    @Bindable
    fun getAddresses(): String? {
        return addresses
    }

    @Bindable
    fun getDnsServers(): String? {
        return dnsServers
    }

    fun getExcludedApplications(): ObservableList<String> {
        return excludedApplications
    }

    @Bindable
    fun getListenPort(): String? {
        return listenPort
    }

    @Bindable
    fun getMtu(): String? {
        return mtu
    }

    @Bindable
    fun getPrivateKey(): String? {
        return privateKey
    }

    @Throws(BadConfigException::class)
    fun resolve(): Interface {
        val builder = Interface.Builder()
        if (!addresses!!.isEmpty())
            builder.parseAddresses(addresses!!)
        if (!dnsServers!!.isEmpty())
            builder.parseDnsServers(dnsServers!!)
        if (!excludedApplications.isEmpty())
            builder.excludeApplications(excludedApplications)
        if (!listenPort!!.isEmpty())
            builder.parseListenPort(listenPort!!)
        if (!mtu!!.isEmpty())
            builder.parseMtu(mtu!!)
        if (!privateKey!!.isEmpty())
            builder.parsePrivateKey(privateKey!!)
        return builder.build()
    }

    fun setAddresses(addresses: String) {
        this.addresses = addresses
        notifyPropertyChanged(BR.addresses)
    }

    fun setDnsServers(dnsServers: String) {
        this.dnsServers = dnsServers
        notifyPropertyChanged(BR.dnsServers)
    }

    fun setListenPort(listenPort: String) {
        this.listenPort = listenPort
        notifyPropertyChanged(BR.listenPort)
    }

    fun setMtu(mtu: String) {
        this.mtu = mtu
        notifyPropertyChanged(BR.mtu)
    }

    fun setPrivateKey(privateKey: String) {
        this.privateKey = privateKey
        publicKey = try {
            KeyPair(Key.fromBase64(privateKey)).publicKey.toBase64()
        } catch (ignored: KeyFormatException) {
            ""
        }

        notifyPropertyChanged(BR.privateKey)
        notifyPropertyChanged(BR.publicKey)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(addresses)
        dest.writeString(dnsServers)
        dest.writeStringList(excludedApplications)
        dest.writeString(listenPort)
        dest.writeString(mtu)
        dest.writeString(privateKey)
        dest.writeString(publicKey)
    }

    private class InterfaceProxyCreator : Parcelable.Creator<InterfaceProxy> {
        override fun createFromParcel(`in`: Parcel): InterfaceProxy {
            return InterfaceProxy(`in`)
        }

        override fun newArray(size: Int): Array<InterfaceProxy?> {
            return arrayOfNulls(size)
        }
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<InterfaceProxy> = InterfaceProxyCreator()
    }
}
