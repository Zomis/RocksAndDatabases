package net.zomis.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {
	public static String md5(String string) {
		return md5(string.getBytes());
	}
	public static String md5(byte[] bytes) {
		if (bytes == null)
			throw new IllegalArgumentException("bytes cannot be null");
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] thedigest = md.digest(bytes);
			return MD5Util.toHEX(thedigest, false);
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

}
