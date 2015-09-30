/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import java.io.Serializable;
import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * <b>uuid</b><br>
 * <br>
 * A <tt>UUIDGenerator</tt> that returns a string of length 32,
 * This string will consist of only hex digits. Optionally,
 * the string may be generated with separators between each
 * component of the UUID.
 * <p/>
 * Mapping parameters supported: separator.
 *
 * @author Gavin King
 */
public class UUIDHexGenerator extends AbstractUUIDGenerator implements Configurable {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( UUIDHexGenerator.class );

	private static boolean WARNED;

	private String sep = "";

	public UUIDHexGenerator() {
		if ( !WARNED ) {
			WARNED = true;
			LOG.usingUuidHexGenerator( this.getClass().getName(), UUIDGenerator.class.getName() );
		}
	}


	@Override
	public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
		sep = ConfigurationHelper.getString( "separator", params, "" );
	}

	@Override
	public Serializable generate(SessionImplementor session, Object obj) {
		return format( getIP() ) + sep
				+ format( getJVM() ) + sep
				+ format( getHiTime() ) + sep
				+ format( getLoTime() ) + sep
				+ format( getCount() );
	}

	protected String format(int intValue) {
		String formatted = Integer.toHexString( intValue );
		StringBuilder buf = new StringBuilder( "00000000" );
		buf.replace( 8 - formatted.length(), 8, formatted );
		return buf.toString();
	}

	protected String format(short shortValue) {
		String formatted = Integer.toHexString( shortValue );
		StringBuilder buf = new StringBuilder( "0000" );
		buf.replace( 4 - formatted.length(), 4, formatted );
		return buf.toString();
	}
}
