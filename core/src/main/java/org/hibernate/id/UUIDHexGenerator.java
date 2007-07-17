//$Id: UUIDHexGenerator.java 8049 2005-08-30 23:28:50Z turin42 $
package org.hibernate.id;

import java.io.Serializable;
import java.util.Properties;

import org.hibernate.Hibernate;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;
import org.hibernate.util.PropertiesHelper;

/**
 * <b>uuid</b><br>
 * <br>
 * A <tt>UUIDGenerator</tt> that returns a string of length 32,
 * This string will consist of only hex digits. Optionally,
 * the string may be generated with separators between each
 * component of the UUID.
 *
 * Mapping parameters supported: separator.
 *
 * @author Gavin King
 */

public class UUIDHexGenerator extends AbstractUUIDGenerator implements Configurable {

	private String sep = "";

	protected String format(int intval) {
		String formatted = Integer.toHexString(intval);
		StringBuffer buf = new StringBuffer("00000000");
		buf.replace( 8-formatted.length(), 8, formatted );
		return buf.toString();
	}

	protected String format(short shortval) {
		String formatted = Integer.toHexString(shortval);
		StringBuffer buf = new StringBuffer("0000");
		buf.replace( 4-formatted.length(), 4, formatted );
		return buf.toString();
	}

	public Serializable generate(SessionImplementor session, Object obj) {
		return new StringBuffer(36)
			.append( format( getIP() ) ).append(sep)
			.append( format( getJVM() ) ).append(sep)
			.append( format( getHiTime() ) ).append(sep)
			.append( format( getLoTime() ) ).append(sep)
			.append( format( getCount() ) )
			.toString();
	}

	public void configure(Type type, Properties params, Dialect d) {
		sep = PropertiesHelper.getString("separator", params, "");
	}

	public static void main( String[] args ) throws Exception {
		Properties props = new Properties();
		props.setProperty("separator", "/");
		IdentifierGenerator gen = new UUIDHexGenerator();
		( (Configurable) gen ).configure(Hibernate.STRING, props, null);
		IdentifierGenerator gen2 = new UUIDHexGenerator();
		( (Configurable) gen2 ).configure(Hibernate.STRING, props, null);

		for ( int i=0; i<10; i++) {
			String id = (String) gen.generate(null, null);
			System.out.println(id);
			String id2 = (String) gen2.generate(null, null);
			System.out.println(id2);
		}

	}

}
