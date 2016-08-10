/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.descriptor.java.FloatTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#FLOAT FLOAT} and {@link Float}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class FloatType extends AbstractSingleColumnStandardBasicType<Float> implements JdbcLiteralFormatter<Float> {
	public static final FloatType INSTANCE = new FloatType();

	public static final Float ZERO = 0.0f;

	public FloatType() {
		super( org.hibernate.type.spi.descriptor.sql.FloatTypeDescriptor.INSTANCE, FloatTypeDescriptor.INSTANCE );
	}
	@Override
	public String getName() {
		return "float";
	}

	@Override
	public JdbcLiteralFormatter<Float> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(Float value, Dialect dialect) {
		return value.toString();
	}
}
