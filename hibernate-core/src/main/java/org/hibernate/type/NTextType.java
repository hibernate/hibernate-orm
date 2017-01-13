/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.StringJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.LongNVarcharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#LONGNVARCHAR LONGNVARCHAR} and {@link String}
 *
 * @author Gavin King,
 * @author Bertrand Renuart
 * @author Steve Ebersole
 */
public class NTextType extends BasicTypeImpl<String> {
	public static final NTextType INSTANCE = new NTextType();

	public NTextType() {
		super( StringJavaDescriptor.INSTANCE, LongNVarcharTypeDescriptor.INSTANCE );
	}

	public String getName() { 
		return "ntext";
	}

	@Override
	public JdbcLiteralFormatter<String> getJdbcLiteralFormatter() {
		// no literal support for LONGNVARCHAR
		return null;
	}
}
