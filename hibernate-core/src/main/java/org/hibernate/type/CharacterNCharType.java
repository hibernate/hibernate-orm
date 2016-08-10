/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.descriptor.java.CharacterTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.NCharTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#NCHAR NCHAR(1)} and {@link Character}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CharacterNCharType
		extends AbstractSingleColumnStandardBasicType<Character> implements JdbcLiteralFormatter<Character> {

	public static final CharacterNCharType INSTANCE = new CharacterNCharType();

	public CharacterNCharType() {
		super( NCharTypeDescriptor.INSTANCE, CharacterTypeDescriptor.INSTANCE );
	}

	public String getName() {
		return "ncharacter";
	}

	@Override
	public JdbcLiteralFormatter<Character> getJdbcLiteralFormatter() {
		return this;
	}

	@Override
	public String toJdbcLiteral(Character value, Dialect dialect) {
		return StringType.INSTANCE.toJdbcLiteral( toString( value ), dialect );
	}
}
