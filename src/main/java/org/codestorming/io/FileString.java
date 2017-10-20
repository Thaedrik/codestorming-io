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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.util.Objects;
import java.util.concurrent.FutureTask;

/**
 * Represents a {@link CharSequence} of a text {@link File file}.
 *
 * @author Thaedrik [thaedrik@codestorming.org]
 */
public class FileString implements CharSequence {

	protected static final int BUFFER_SIZE = 50;

	protected static final double LOAD_FACTOR = 0.75;

	protected File file;

	/**
	 * The input stream to load.
	 */
	protected InputStream inputStream;

	private Charset charset;

	private char[] content;

	private volatile int size = 0;

	private FutureTask<Boolean> loader;

	/** This {@code FileString}'s hashcode */
	private int hash;

	/**
	 * Creates a new {@code FileString} with the given {@link File file}.
	 *
	 * @param file The file to load as a {@link CharSequence}.
	 * @param charset The {@link Charset} to use when loading the file.
	 * @throws NullPointerException if the given file or charset is {@code null}.
	 * @throws IllegalArgumentException if the given file is not a <em>normal file</em>.
	 * @see File#isFile()
	 */
	public FileString(File file, Charset charset) {
		this.file = Objects.requireNonNull(file);
		this.charset = Objects.requireNonNull(charset);
		if (!file.exists()) {
			throw new IllegalArgumentException("The given file doesn't exist.");
		}// else
		if (!file.isFile()) {
			throw new IllegalArgumentException("The given file is not a 'normal' file");
		}// else

		load();
	}

	/**
	 * Creates a new {@code FileString} with the given {@link InputStream}.
	 *
	 * @param input The {@link InputStream} used for loading the {@link CharSequence}.
	 * @param charset The {@link Charset} to use when reading the input stream.
	 * @throws NullPointerException if the given stream or charset is {@code null}.
	 */
	public FileString(InputStream input, Charset charset) {
		this.inputStream = Objects.requireNonNull(input);
		this.charset = Objects.requireNonNull(charset);

		load();
	}

	/**
	 * Creates a new {@code FileString} with the given {@link File file} and UTF-8 encoding.
	 *
	 * @param file The file to load as a {@link CharSequence}.
	 * @throws NullPointerException if the given file is {@code null}.
	 * @throws IllegalArgumentException if the given file is not a <em>normal file</em>.
	 * @see File#isFile()
	 */
	public FileString(File file) {
		this(file, Charset.forName("UTF-8"));
	}

	private synchronized void load() {
		loader = new FutureTask<>(() -> {
			final CharsetDecoder decoder = charset.newDecoder()
												  .onMalformedInput(CodingErrorAction.REPLACE)
												  .onUnmappableCharacter(CodingErrorAction.REPLACE);
			InputStream input = null;
			byte[] buffer = new byte[1024];
			try {
				content = new char[FileString.this.getInitialCapacity()];
				if (file != null) {
					input = new FileInputStream(file);
				} else if (inputStream != null) {
					input = inputStream;
				}
				int len;
				while ((len = input.read(buffer)) > 0) {
					final CharBuffer charBuffer = decoder.decode(ByteBuffer.wrap(buffer, 0, len));
					final int length = charBuffer.length();
					FileString.this.ensureCapacity(size + length);
					charBuffer.get(content, size, length);
					size += length;
				}
				return true;
			} catch (IOException e) {
				System.err.println(e.getMessage());
				return false;
			} finally {
				FileHelper.close(input);
			}
		});
		loader.run();
	}

	/**
	 * Ensures the capacity of the {@code content} array can accept {@link #BUFFER_SIZE} new elements.
	 */
	private void ensureCapacity(int minimumCapacity) {
		if (content.length < minimumCapacity) {
			int newCapacity = (int) ((content.length) * (1 + LOAD_FACTOR));
			if (newCapacity < minimumCapacity) {
				newCapacity = minimumCapacity;
			}
			char[] newContent = new char[newCapacity];
			System.arraycopy(content, 0, newContent, 0, size);
			content = newContent;
		}
	}

	/**
	 * Returns the initial capacity of this char sequence.
	 *
	 * @return the initial capacity of this char sequence.
	 */
	private int getInitialCapacity() {
		if (file == null) {
			return 1024;
		}// else
		long length = file.length();
		if (length > Integer.MAX_VALUE) {
			length = Integer.MAX_VALUE / 2;
		} else if (length < BUFFER_SIZE) {
			length = BUFFER_SIZE;
		} else {
			length /= 2;
		}
		return (int) length;
	}

	@Override
	public int length() {
		if (!ready()) {
			return 0;
		}// else
		return size;
	}

	@Override
	public synchronized char charAt(int index) {
		if (index < 0 || index >= size) {
			throw new ArrayIndexOutOfBoundsException(index);
		}// else
		if (!ready()) {
			return 0;
		}// else
		return content[index];
	}

	@Override
	public synchronized CharSequence subSequence(int start, int end) {
		if (end < start) {
			throw new IllegalArgumentException("The given end value cannot be lesser than the start index.");
		}// else
		if (!ready()) {
			return null;
		}// else
		return new String(content, start, end - start);
	}

	/**
	 * Replaces the substring by the given replacement string.
	 * <p>
	 * The substring begin at the given {@code start} index and extends to {@code end - 1}.
	 *
	 * @param start The index of the first character of the sustring to replace.
	 * @param end The end of the substring to replace.
	 * @param replacement The replacement string.
	 */
	public synchronized void replace(int start, int end, String replacement) {
		if (!ready()) {
			return;
		}// else

		int length = end - start;
		int offset = replacement.length() - length;
		if (offset != 0) {
			char[] newContent = new char[size + offset];
			System.arraycopy(content, 0, newContent, 0, start);
			System.arraycopy(content, end, newContent, start + replacement.length(), size - end);
			content = newContent;
		}
		replacement.getChars(0, replacement.length(), content, start);
		size += offset;
	}

	/**
	 * Flush the content into the file.
	 * <p>
	 * This method will do nothing if this {@code FileString} has been created from an {@link InputStream}.
	 *
	 * @throws IOException if an error occurs while writing to the file.
	 */
	public synchronized void flush() throws IOException {
		if (file == null || !ready()) {
			return;
		}// else

		try (FileOutputStream fos = new FileOutputStream(file, false)) {
			fos.write(toString().getBytes(charset));
		}
	}

	@Override
	public String toString() {
		if (!ready()) {
			return "FILE-STRING NOT LOADED";
		}// else
		return new String(content, 0, size);
	}

	@Override
	public boolean equals(Object anObject) {
		if (!ready()) {
			return false;
		}// else
		if (this == anObject) {
			return true;
		}// else
		if (!(anObject instanceof CharSequence)) {
			return false;
		}// else
		final CharSequence other = (CharSequence) anObject;
		if (size != other.length()) {
			return false;
		}// else
		final char[] chars = content;
		for (int i = 0, n = size; i < n; i++) {
			if (chars[i] != other.charAt(i)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		if (!ready()) {
			return 0;
		}// else
		int h = hash;
		if (h == 0 && content.length > 0) {
			final char val[] = content;
			for (int i = 0, n = val.length; i < n; i++) {
				h = 31 * h + val[i];
			}
			hash = h;
		}
		return h;
	}

	/**
	 * Indicates if this sequence is correctly loaded.
	 * <p>
	 * If this sequence is currently loading, this method will wait until the end.
	 *
	 * @return {@code true} if the sequence is fully loaded;<br> {@code false} otherwise.
	 */
	protected boolean ready() {
		try {
			return loader.get();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			return false;
		}
	}
}