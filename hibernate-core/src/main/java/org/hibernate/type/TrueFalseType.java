/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.descriptor.java.BooleanTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.CharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#CHAR CHAR(1)} and {@link Boolean} (using 'T' and 'F')
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class TrueFalseType extends AbstractSingleColumnStandardBasicType<Boolean>
		implements JdbcLiteralFormatter<Boolean> {

	public static final TrueFalseType INSTANCE = new TrueFalseType();

	public TrueFalseType() {
		super( CharTypeDescriptor.INSTANCE, new BooleanTypeDescriptor( 'T', 'F' ) );
	}

	@Override
	public String getName() {
		return "true_false";
	}

	@Override
	public JdbcLiteralFormatter<Boolean> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(Boolean value, Dialect dialect) {
		return StringType.INSTANCE.toJdbcLiteral( value ? "T" : "F", dialect );
	}
}
