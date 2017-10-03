/*
 * Copyright (c) 2012-2017 Codestorming.org
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Codestorming - initial API and implementation
 */
package org.codestorming.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Utility class with I/O methods.
 *
 * @author Thaedrik [thaedrik@codestorming.org]
 */
public class IOUtil {

	// Suppressing default constructor, ensuring non instantiability
	private IOUtil() {
	}

	/**
	 * Creates a {@code String} with an {@link InputStream}.
	 *
	 * @param inputStream {@code InputStream} from which to create the String.
	 * @param charsetName The name of the charset to use to decode the stream's bytes.
	 * @return the {@code String} created with the given {@link InputStream}.
	 * @throws UnsupportedEncodingException if the named charset is not supported.
	 * @throws IOException If a problem occurs while reading the {@link InputStream}.
	 */
	public static String createStringFromInputStream(InputStream inputStream, String charsetName) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len;
		while ((len = inputStream.read(buffer)) > 0) {
			baos.write(buffer, 0, len);
		}
		if (charsetName != null) {
			return baos.toString(charsetName);
		} else {
			return baos.toString();
		}
	}

	/**
	 * Creates a {@code String} with an {@link InputStream}.<br> (Use the default system charset for decoding the byte
	 * stream).
	 *
	 * @param inputStream {@code InputStream} from which to create the String.
	 * @return the {@code String} created with the given {@link InputStream}.
	 * @throws IOException If a problem occurs while reading the {@link InputStream}.
	 */
	public static String createStringFromInputStream(InputStream inputStream) throws IOException {
		return createStringFromInputStream(inputStream, null);
	}
}
