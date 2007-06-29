//$Id: ReaderInputStream.java 5683 2005-02-12 03:09:22Z oneovthafew $
package org.hibernate.lob;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Exposes a <tt>Reader</tt> as an <tt>InputStream</tt>
 * @author Gavin King
 */
public class ReaderInputStream extends InputStream {
	
	private Reader reader;
	
	public ReaderInputStream(Reader reader) {
		this.reader = reader;
	}
	
	public int read() throws IOException {
		return reader.read();
	}
	
}
