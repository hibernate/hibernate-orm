/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.id;

import java.io.Serializable;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final Logger log = LoggerFactory.getLogger( UUIDHexGenerator.class );
	private static boolean warned = false;

	private String sep = "";

	public UUIDHexGenerator() {
		if ( ! warned ) {
			warned = true;
			log.warn(
					"Using {} which does not generate IETF RFC 4122 compliant UUID values; consider using {} instead",
					this.getClass().getName(),
					UUIDGenerator.class.getName()
			);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void configure(Type type, Properties params, Dialect d) {
		sep = PropertiesHelper.getString( "separator", params, "" );
	}

	/**
	 * {@inheritDoc}
	 */
	public Serializable generate(SessionImplementor session, Object obj) {
		return new StringBuffer( 36 )
				.append( format( getIP() ) ).append( sep )
				.append( format( getJVM() ) ).append( sep )
				.append( format( getHiTime() ) ).append( sep )
				.append( format( getLoTime() ) ).append( sep )
				.append( format( getCount() ) )
				.toString();
	}

	protected String format(int intValue) {
		String formatted = Integer.toHexString( intValue );
		StringBuffer buf = new StringBuffer( "00000000" );
		buf.replace( 8 - formatted.length(), 8, formatted );
		return buf.toString();
	}

	protected String format(short shortValue) {
		String formatted = Integer.toHexString( shortValue );
		StringBuffer buf = new StringBuffer( "0000" );
		buf.replace( 4 - formatted.length(), 4, formatted );
		return buf.toString();
	}
}
