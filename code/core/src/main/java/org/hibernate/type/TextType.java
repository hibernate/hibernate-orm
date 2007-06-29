//$Id: TextType.java 4582 2004-09-25 11:22:20Z oneovthafew $
package org.hibernate.type;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;

/**
 * <tt>text</tt>: A type that maps an SQL CLOB to a Java String.
 * @author Gavin King, Bertrand Renuart
 */
public class TextType extends ImmutableType {

	public void set(PreparedStatement st, Object value, int index) throws HibernateException, SQLException {
		String str = (String) value;
		st.setCharacterStream( index, new StringReader(str), str.length() );
	}

	public Object get(ResultSet rs, String name) throws HibernateException, SQLException {

			// Retrieve the value of the designated column in the current row of this
			// ResultSet object as a java.io.Reader object
			Reader charReader = rs.getCharacterStream(name);

			// if the corresponding SQL value is NULL, the reader we got is NULL as well
			if (charReader==null) return null;

			// Fetch Reader content up to the end - and put characters in a StringBuffer
			StringBuffer sb = new StringBuffer();
			try {
				char[] buffer = new char[2048];
				while (true) {
					int amountRead = charReader.read(buffer, 0, buffer.length);
					if ( amountRead == -1 ) break;
					sb.append(buffer, 0, amountRead);
				}
			}
			catch (IOException ioe) {
				throw new HibernateException( "IOException occurred reading text", ioe );
			}
			finally {
				try {
					charReader.close();
				}
				catch (IOException e) {
					throw new HibernateException( "IOException occurred closing stream", e );
				}
			}

			// Return StringBuffer content as a large String
			return sb.toString();
	}

	public int sqlType() {
		return Types.CLOB; //or Types.LONGVARCHAR?
	}

	public Class getReturnedClass() {
		return String.class;
	}

	public String getName() { return "text"; }

	public String toString(Object val) {
		return (String) val;
	}
	public Object fromStringValue(String xml) {
		return xml;
	}

}







