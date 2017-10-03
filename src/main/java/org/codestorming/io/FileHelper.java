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

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.MessageFormat;

/**
 * Helper providing convenient methods for dealing with {@link File files}.
 *
 * @author Thaedrik [thaedrik@codestorming.org]
 */
public class FileHelper {

	/**
	 * Closes the given {@link Closeable} without raising exceptions if any.
	 *
	 * @param closeable The closeable to close (may be {@code null}).
	 */
	public static void close(Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (Exception e) {
			// Ignore
		}
	}

	/**
	 * Checks if the given file exists and throws a {@link FileNotFoundException} is it doesn't.
	 *
	 * @param file The file to check.
	 * @throws FileNotFoundException if the given {@code file} doesn't exist or if it is {@code null}.
	 */
	public static void checkFileExists(File file) throws FileNotFoundException {
		if (file == null) {
			throw new FileNotFoundException();
		}// else
		if (!file.exists()) {
			String message = "The file {0} does not exist.";
			throw new FileNotFoundException(MessageFormat.format(message, file.getName()));
		}
	}

	// Suppressing default constructor, ensuring non instantiability
	private FileHelper() {}
}