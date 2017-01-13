/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.net.URL;

import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.UrlJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.VarcharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link URL}
 *
 * @author Steve Ebersole
 */
public class UrlType extends BasicTypeImpl<URL> {
	public static final UrlType INSTANCE = new UrlType();

	public UrlType() {
		super( UrlJavaDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "url";
	}

	@Override
	public JdbcLiteralFormatter<URL> getJdbcLiteralFormatter() {
		return VarcharTypeDescriptor.INSTANCE.getJdbcLiteralFormatter( UrlJavaDescriptor.INSTANCE );
	}

	@Override
	public String toString(URL value) {
		return UrlJavaDescriptor.INSTANCE.toString( value );
	}
}
