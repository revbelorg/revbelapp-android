package com.news.revbel.utilities;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

public class ProxyUtilities {
    public static SocketFactory torSocksFactory() {
        return new SocketFactory() {
            @Override
            public Socket createSocket() throws IOException {
                Proxy proxy = new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(
                        "127.0.0.1", 9050));
                return new Socket(proxy);
            }

            @Override
            public Socket createSocket(String s, int i) throws IOException, UnknownHostException {
                return null;
            }

            @Override
            public Socket createSocket(String s, int i, InetAddress inetAddress, int i1) throws IOException, UnknownHostException {
                return null;
            }

            @Override
            public Socket createSocket(InetAddress inetAddress, int i) throws IOException {
                return null;
            }

            @Override
            public Socket createSocket(InetAddress inetAddress, int i, InetAddress inetAddress1, int i1) throws IOException {
                return null;
            }
        };
    }

}
