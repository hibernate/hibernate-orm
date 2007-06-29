//$Id: $
package org.hibernate.type;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.io.Reader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.CharArrayReader;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;

/**
 * Logic to bind stream of char into a VARCHAR
 *
 * @author Emmanuel Bernard
 */
public abstract class AbstractCharArrayType extends MutableType {

	/**
	 * Convert the char[] into the expected object type
	 */
	abstract protected Object toExternalFormat(char[] chars);

	/**
	 * Convert the object into the internal char[] representation
	 */
	abstract protected char[] toInternalFormat(Object chars);

	public Object get(ResultSet rs, String name) throws SQLException {
		Reader stream = rs.getCharacterStream(name);
		if ( stream == null ) return toExternalFormat( null );
		CharArrayWriter writer = new CharArrayWriter();
		for(;;) {
			try {
				int c = stream.read();
				if ( c == -1) return toExternalFormat( writer.toCharArray() );
				writer.write( c );
			}
			catch (IOException e) {
				throw new HibernateException("Unable to read character stream from rs");
			}
		}
	}

	public abstract Class getReturnedClass();

	public void set(PreparedStatement st, Object value, int index) throws SQLException {
		char[] chars = toInternalFormat( value );
		st.setCharacterStream(index, new CharArrayReader(chars), chars.length);
	}

	public int sqlType() {
		return Types.VARCHAR;
	}

	public String objectToSQLString(Object value, Dialect dialect) throws Exception {

		return '\'' + new String( toInternalFormat( value ) ) + '\'';
	}

	public Object stringToObject(String xml) throws Exception {
		if (xml == null) return toExternalFormat( null );
		int length = xml.length();
		char[] chars = new char[length];
		for (int index = 0 ; index < length ; index++ ) {
			chars[index] = xml.charAt( index );
		}
		return toExternalFormat( chars );
	}

	public String toString(Object value) {
		if (value == null) return null;
		return new String( toInternalFormat( value ) );
	}

	public Object fromStringValue(String xml) {
		if (xml == null) return null;
		int length = xml.length();
		char[] chars = new char[length];
		for (int index = 0 ; index < length ; index++ ) {
			chars[index] = xml.charAt( index );
		}
		return toExternalFormat( chars );
	}

	protected Object deepCopyNotNull(Object value) throws HibernateException {
		char[] chars = toInternalFormat(value);
		char[] result = new char[chars.length];
		System.arraycopy(chars, 0, result, 0, chars.length);
		return toExternalFormat(result);
	}
}
