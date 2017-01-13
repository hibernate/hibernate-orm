/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.DoubleJavaDescriptor;

/**
 * A type that maps between {@link java.sql.Types#DOUBLE DOUBLE} and {@link Double}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DoubleType extends BasicTypeImpl<Double> {
	public static final DoubleType INSTANCE = new DoubleType();

	public static final Double ZERO = 0.0;

	public DoubleType() {
		super( DoubleJavaDescriptor.INSTANCE, org.hibernate.type.spi.descriptor.sql.DoubleTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "double";
	}

	@Override
	public JdbcLiteralFormatter<Double> getJdbcLiteralFormatter() {
		return org.hibernate.type.spi.descriptor.sql.DoubleTypeDescriptor.INSTANCE.getJdbcLiteralFormatter(
				DoubleJavaDescriptor.INSTANCE
		);
	}
}
