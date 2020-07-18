/**
 * @author wangjianbo
 * @created 2018-10-10
 */
package com.iamnotme.utils;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * 自定义的信任管理器
 */
public class MyX509TrustManager implements X509TrustManager {
    /**
     * 检查客户端证书，若不信任，抛出异常
     */
    public void checkClientTrusted(X509Certificate[] arg0, String arg1)
            throws CertificateException {
    }
    /**
     * 检查服务端证书，若不信任，抛出异常，反之，若不抛出异常，则表示信任（所以，空方法代表信任所有的服务端证书）
     */
    public void checkServerTrusted(X509Certificate[] arg0, String arg1)
            throws CertificateException {
    }
    /**
     * 返回受信任的X509证书数组
     */
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}