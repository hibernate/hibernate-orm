/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.spi.descriptor.java.CharacterArrayTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.VarcharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#VARCHAR VARCHAR} and {@link Character Character[]}
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class CharacterArrayType extends BasicTypeImpl<Character[]>
		implements JdbcLiteralFormatter<Character[]> {
	public static final CharacterArrayType INSTANCE = new CharacterArrayType();

	public CharacterArrayType() {
		super( CharacterArrayTypeDescriptor.INSTANCE, VarcharTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "wrapper-characters";
	}

	@Override
	public JdbcLiteralFormatter<Character[]> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(Character[] value, Dialect dialect) {
		return StringType.INSTANCE.toJdbcLiteral( toString( value ), dialect );
	}
}
