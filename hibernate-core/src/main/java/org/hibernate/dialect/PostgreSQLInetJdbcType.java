/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Christian Beikov
 */
public class PostgreSQLInetJdbcType extends PostgreSQLPGObjectJdbcType {

	public static final PostgreSQLInetJdbcType INSTANCE = new PostgreSQLInetJdbcType();

	public PostgreSQLInetJdbcType() {
		super( "inet", SqlTypes.INET );
	}

	@Override
	protected <X> X fromString(String string, JavaType<X> javaType, WrapperOptions options) {
		final String host;
		if ( string == null ) {
			host = null;
		}
		else {
			// The default toString representation of the inet type adds the subnet mask
			final int slashIndex = string.lastIndexOf( '/' );
			if ( slashIndex == -1 ) {
				host = string;
			}
			else {
				host = string.substring( 0, slashIndex );
			}
		}
		return javaType.wrap( host, options );
	}
}
