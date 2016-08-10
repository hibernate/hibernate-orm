/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.net.URL;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.descriptor.java.UrlTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.VarcharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link URL}
 *
 * @author Steve Ebersole
 */
public class UrlType extends AbstractSingleColumnStandardBasicType<URL> implements JdbcLiteralFormatter<URL> {
	public static final UrlType INSTANCE = new UrlType();

	public UrlType() {
		super( VarcharTypeDescriptor.INSTANCE, UrlTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "url";
	}

	@Override
	public JdbcLiteralFormatter<URL> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toString(URL value) {
		return UrlTypeDescriptor.INSTANCE.toString( value );
	}

	@Override
	public String toJdbcLiteral(URL value, Dialect dialect) {
		return StringType.INSTANCE.toJdbcLiteral( toString( value ), dialect );
	}
}
