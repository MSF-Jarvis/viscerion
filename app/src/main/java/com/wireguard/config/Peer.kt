@file:Suppress("DefaultLocale") // String.format warns for this but configs are not locale-oriented
package com.wireguard.config

import android.os.Parcel
import android.os.Parcelable
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.library.baseAdapters.BR
import com.wireguard.android.Application
import com.wireguard.android.R
import com.wireguard.crypto.KeyEncoding
import java9.lang.Iterables
import java.net.Inet6Address
import java.net.InetSocketAddress
import java.net.URI
import java.net.URISyntaxException
import java.net.UnknownHostException
import java.util.Arrays

class Peer {
    private val allowedIPsList: MutableList<InetNetwork> = ArrayList()
    private var endpoint: InetSocketAddress? = null
    private var persistentKeepalive: Int = 0
    private var preSharedKey: String? = null
    private var publicKey: String? = null
    private val context = Application.get()

    private fun addAllowedIPs(allowedIPs: Array<String>?) {
        if (allowedIPs != null && allowedIPs.isNotEmpty()) {
            for (allowedIP in allowedIPs) {
                allowedIPsList.add(InetNetwork(allowedIP))
            }
        }
    }

    fun getAllowedIPs(): Array<InetNetwork> {
        return allowedIPsList.toTypedArray()
    }

    private fun getAllowedIPsString(): String? {
        return if (allowedIPsList.isEmpty()) null else Attribute.iterableToString(allowedIPsList)
    }

    fun getEndpoint(): InetSocketAddress? {
        return endpoint
    }

    private fun getEndpointString(): String? {
        if (endpoint == null)
            return null
        return if (endpoint!!.hostString.contains(":") &&
                !endpoint!!.hostString.contains("["))
            String.format("[%s]:%d", endpoint!!.hostString, endpoint!!.port)
        else String.format("%s:%d", endpoint!!.hostString, endpoint!!.port)
    }

    fun getPersistentKeepalive(): Int {
        return persistentKeepalive
    }

    private fun getPersistentKeepaliveString(): String? {
        return if (persistentKeepalive == 0) null else Integer.valueOf(persistentKeepalive).toString()
    }

    fun getPreSharedKey(): String? {
        return preSharedKey
    }

    fun getPublicKey(): String? {
        return publicKey
    }

    @Throws(UnknownHostException::class)
    fun getResolvedEndpointString(): String {
        if (endpoint == null)
            throw UnknownHostException("{empty}")
        if (endpoint!!.isUnresolved)
            endpoint = InetSocketAddress(endpoint!!.hostString, endpoint!!.port)
        if (endpoint!!.isUnresolved)
            throw UnknownHostException(endpoint!!.hostString)
        return if (endpoint!!.address is Inet6Address) String.format("[%s]:%d",
                endpoint!!.address.hostAddress,
                endpoint!!.port) else String.format("%s:%d",
                endpoint!!.address.hostAddress,
                endpoint!!.port)
    }

    fun parse(line: String) {
        val key = Attribute.match(line)
                ?: throw IllegalArgumentException(context.getString(R.string.tunnel_error_interface_parse_failed, line))
        when (key) {
            Attribute.ALLOWED_IPS -> addAllowedIPs(key.parseList(line))
            Attribute.ENDPOINT -> setEndpointString(key.parse(line))
            Attribute.PERSISTENT_KEEPALIVE -> setPersistentKeepaliveString(key.parse(line))
            Attribute.PRESHARED_KEY -> setPreSharedKey(key.parse(line))
            Attribute.PUBLIC_KEY -> setPublicKey(key.parse(line))
            else -> throw IllegalArgumentException(line)
        }
    }

    private fun setAllowedIPsString(allowedIPsString: String?) {
        allowedIPsList.clear()
        addAllowedIPs(Attribute.stringToList(allowedIPsString))
    }

    private fun setEndpoint(endpoint: InetSocketAddress?) {
        this.endpoint = endpoint
    }

    private fun setEndpointString(endpoint: String?) {
        if (endpoint != null && !endpoint.isEmpty()) {
            val constructedEndpoint: InetSocketAddress
            if (endpoint.indexOf('/') != -1 || endpoint.indexOf('?') != -1 || endpoint.indexOf('#') != -1)
                throw IllegalArgumentException(context.getString(R.string.tunnel_error_forbidden_endpoint_chars))
            val uri: URI
            try {
                uri = URI("wg://$endpoint")
            } catch (e: URISyntaxException) {
                throw IllegalArgumentException(e)
            }

            constructedEndpoint = InetSocketAddress.createUnresolved(uri.host, uri.port)
            setEndpoint(constructedEndpoint)
        } else
            setEndpoint(null)
    }

    private fun setPersistentKeepalive(persistentKeepalive: Int) {
        this.persistentKeepalive = persistentKeepalive
    }

    private fun setPersistentKeepaliveString(persistentKeepalive: String?) {
        if (persistentKeepalive != null && !persistentKeepalive.isEmpty())
            setPersistentKeepalive(Integer.parseInt(persistentKeepalive, 10))
        else
            setPersistentKeepalive(0)
    }

    private fun setPreSharedKey(preSharedKey: String?) {
        var key = preSharedKey
        if (key != null && key.isEmpty())
            key = null
        if (key != null)
            KeyEncoding.keyFromBase64(key)
        this.preSharedKey = key
    }

    private fun setPublicKey(publicKey: String?) {
        var key = publicKey
        if (key != null && key.isEmpty())
            key = null
        if (key != null)
            KeyEncoding.keyFromBase64(key)
        this.publicKey = key
    }

    override fun toString(): String {
        val sb = StringBuilder().append("[Peer]\n")
        if (!allowedIPsList.isEmpty())
            sb.append(Attribute.ALLOWED_IPS.composeWith(allowedIPsList))
        if (endpoint != null)
            sb.append(Attribute.ENDPOINT.composeWith(getEndpointString()))
        if (persistentKeepalive != 0)
            sb.append(Attribute.PERSISTENT_KEEPALIVE.composeWith(persistentKeepalive))
        if (preSharedKey != null)
            sb.append(Attribute.PRESHARED_KEY.composeWith(preSharedKey))
        if (publicKey != null)
            sb.append(Attribute.PUBLIC_KEY.composeWith(publicKey))
        return sb.toString()
    }

    class Observable : BaseObservable, Parcelable {
        private var allowedIPs: String? = null
        private var endpoint: String? = null
        private var persistentKeepalive: String? = null
        private var preSharedKey: String? = null
        private var publicKey: String? = null
        private val interfaceDNSRoutes = ArrayList<String>()
        private var numSiblings: Int = 0

        val canToggleExcludePrivateIPs: Boolean
            @Bindable
            get() {
                val ips = Arrays.asList(*Attribute.stringToList(allowedIPs))
                return numSiblings == 0 && (ips.contains(DEFAULT_ROUTE_V4) || ips.containsAll(DEFAULT_ROUTE_MOD_RFC1918_V4))
            }

        val isExcludePrivateIPsOn: Boolean
            @Bindable
            get() =
                numSiblings == 0 && Arrays.asList(*Attribute.stringToList(allowedIPs)).containsAll(DEFAULT_ROUTE_MOD_RFC1918_V4)

        constructor(parent: Peer) {
            loadData(parent)
        }

        private constructor(`in`: Parcel) {
            allowedIPs = `in`.readString()
            endpoint = `in`.readString()
            persistentKeepalive = `in`.readString()
            preSharedKey = `in`.readString()
            publicKey = `in`.readString()
            numSiblings = `in`.readInt()
            `in`.readStringList(interfaceDNSRoutes)
        }

        fun commitData(parent: Peer) {
            parent.setAllowedIPsString(allowedIPs)
            parent.setEndpointString(endpoint)
            parent.setPersistentKeepaliveString(persistentKeepalive)
            parent.setPreSharedKey(preSharedKey)
            parent.setPublicKey(publicKey)
            if (parent.publicKey == null)
                throw IllegalArgumentException(Application.get().getString(R.string.tunnel_error_empty_peer_public_key))
            loadData(parent)
            notifyChange()
        }

        override fun describeContents(): Int {
            return 0
        }

        fun toggleExcludePrivateIPs() {
            val ips = HashSet(Arrays.asList(*Attribute.stringToList(allowedIPs)))
            val hasDefaultRoute = ips.contains(DEFAULT_ROUTE_V4)
            val hasDefaultRouteModRFC1918 = ips.containsAll(DEFAULT_ROUTE_MOD_RFC1918_V4)
            if (!hasDefaultRoute && !hasDefaultRouteModRFC1918 || numSiblings > 0)
                return
            Iterables.removeIf(ips) { ip -> !ip.contains(":") }
            if (hasDefaultRoute) {
                ips.addAll(DEFAULT_ROUTE_MOD_RFC1918_V4)
                ips.addAll(interfaceDNSRoutes)
            } else if (hasDefaultRouteModRFC1918)
                ips.add(DEFAULT_ROUTE_V4)
            setAllowedIPs(Attribute.iterableToString(ips))
        }

        @Bindable
        fun getAllowedIPs(): String? {
            return allowedIPs
        }

        @Bindable
        fun getEndpoint(): String? {
            return endpoint
        }

        @Bindable
        fun getPersistentKeepalive(): String? {
            return persistentKeepalive
        }

        @Bindable
        fun getPreSharedKey(): String? {
            return preSharedKey
        }

        @Bindable
        fun getPublicKey(): String? {
            return publicKey
        }

        private fun loadData(parent: Peer) {
            allowedIPs = parent.getAllowedIPsString()
            endpoint = parent.getEndpointString()
            persistentKeepalive = parent.getPersistentKeepaliveString()
            preSharedKey = parent.preSharedKey
            publicKey = parent.publicKey
        }

        fun setAllowedIPs(allowedIPs: String) {
            this.allowedIPs = allowedIPs
            notifyPropertyChanged(BR.allowedIPs)
            notifyPropertyChanged(BR.canToggleExcludePrivateIPs)
            notifyPropertyChanged(BR.isExcludePrivateIPsOn)
        }

        fun setEndpoint(endpoint: String) {
            this.endpoint = endpoint
            notifyPropertyChanged(BR.endpoint)
        }

        fun setPersistentKeepalive(persistentKeepalive: String) {
            this.persistentKeepalive = persistentKeepalive
            notifyPropertyChanged(BR.persistentKeepalive)
        }

        fun setPreSharedKey(preSharedKey: String) {
            this.preSharedKey = preSharedKey
            notifyPropertyChanged(BR.preSharedKey)
        }

        fun setPublicKey(publicKey: String) {
            this.publicKey = publicKey
            notifyPropertyChanged(BR.publicKey)
        }

        fun setInterfaceDNSRoutes(dnsServers: String?) {
            val ips = HashSet(Arrays.asList(*Attribute.stringToList(allowedIPs)))
            val modifyAllowedIPs = ips.containsAll(DEFAULT_ROUTE_MOD_RFC1918_V4)

            ips.removeAll(interfaceDNSRoutes)
            interfaceDNSRoutes.clear()
            for (dnsServer in Attribute.stringToList(dnsServers)) {
                if (!dnsServer.contains(":"))
                    interfaceDNSRoutes.add("$dnsServer/32")
            }
            ips.addAll(interfaceDNSRoutes)
            if (modifyAllowedIPs)
                setAllowedIPs(Attribute.iterableToString(ips))
        }

        fun setNumSiblings(num: Int) {
            numSiblings = num
            notifyPropertyChanged(BR.canToggleExcludePrivateIPs)
            notifyPropertyChanged(BR.isExcludePrivateIPsOn)
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(allowedIPs)
            dest.writeString(endpoint)
            dest.writeString(persistentKeepalive)
            dest.writeString(preSharedKey)
            dest.writeString(publicKey)
            dest.writeInt(numSiblings)
            dest.writeStringList(interfaceDNSRoutes)
        }

        companion object {
            val CREATOR: Parcelable.Creator<Observable> = object : Parcelable.Creator<Observable> {
                override fun createFromParcel(`in`: Parcel): Observable {
                    return Observable(`in`)
                }

                override fun newArray(size: Int): Array<Observable?> {
                    return arrayOfNulls(size)
                }
            }

            fun newInstance(): Observable {
                return Observable(Peer())
            }

            private const val DEFAULT_ROUTE_V4 = "0.0.0.0/0"
            private val DEFAULT_ROUTE_MOD_RFC1918_V4 = Arrays.asList("0.0.0.0/5", "8.0.0.0/7", "11.0.0.0/8", "12.0.0.0/6", "16.0.0.0/4", "32.0.0.0/3", "64.0.0.0/2", "128.0.0.0/3", "160.0.0.0/5", "168.0.0.0/6", "172.0.0.0/12", "172.32.0.0/11", "172.64.0.0/10", "172.128.0.0/9", "173.0.0.0/8", "174.0.0.0/7", "176.0.0.0/4", "192.0.0.0/9", "192.128.0.0/11", "192.160.0.0/13", "192.169.0.0/16", "192.170.0.0/15", "192.172.0.0/14", "192.176.0.0/12", "192.192.0.0/10", "193.0.0.0/8", "194.0.0.0/7", "196.0.0.0/6", "200.0.0.0/5", "208.0.0.0/4")
        }
    }
}
