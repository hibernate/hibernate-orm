/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.CharacterJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.CharSqlDescriptor;

/**
 * A type that maps between {@link java.sql.Types#CHAR CHAR(1)} and {@link Character}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class CharacterType extends BasicTypeImpl<Character> {

	public static final CharacterType INSTANCE = new CharacterType();

	protected CharacterType() {
		super( CharacterJavaDescriptor.INSTANCE, CharSqlDescriptor.INSTANCE );
	}

	public String getName() {
		return "character";
	}

	@Override
	public JdbcLiteralFormatter<Character> getJdbcLiteralFormatter() {
		return CharSqlDescriptor.INSTANCE.getJdbcLiteralFormatter( CharacterJavaDescriptor.INSTANCE );
	}
}
