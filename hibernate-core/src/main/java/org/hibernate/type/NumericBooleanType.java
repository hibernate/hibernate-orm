/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.descriptor.java.BooleanTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.IntegerTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#INTEGER INTEGER} and {@link Boolean} (using 1 and 0)
 *
 * @author Steve Ebersole
 */
public class NumericBooleanType 
		extends AbstractSingleColumnStandardBasicType<Boolean> implements JdbcLiteralFormatter<Boolean> {

	public static final NumericBooleanType INSTANCE = new NumericBooleanType();

	protected NumericBooleanType() {
		super( IntegerTypeDescriptor.INSTANCE, BooleanTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "numeric_boolean";
	}

	@Override
	public JdbcLiteralFormatter<Boolean> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(Boolean value, Dialect dialect) {
		// See note on BooleanType#toJdbcLiteral
		return value ? "1" : "0";
	}
}
