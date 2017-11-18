package org.hibernate.engine.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import org.junit.Test;

/**
 * @author SW Tang
 */
public class ReaderInputStreamTest {
	private static final String TEST_STRING = "abc \u00F1 \u5510";
	private static final int EOF = -1;

	@Test
	public void testRead() throws IOException {
		StringBuffer strb = new StringBuffer();
		for ( int i = 0; i < 1000; i++ ) {
			strb.append( TEST_STRING );
		}
		String testStr = strb.toString();

		byte[] bs = testStr.getBytes( "UTF-8" ); // TODO Charset should be configurable
		InputStream in = new ReaderInputStream( new StringReader( testStr ) );
		int c;
		for ( byte b : bs ) {
			c = in.read();
			assert c != EOF;
			assert c >= 0;
			assert c <= 255;
			assert new Integer( 0xff & b ).equals( c );
		}
	}
}
