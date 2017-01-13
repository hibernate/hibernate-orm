/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.CharacterArrayJavaDescriptor;
import org.hibernate.type.spi.descriptor.sql.ClobTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#CLOB CLOB} and {@link Character Character[]}
 * <p/>
 * Essentially a {@link MaterializedClobType} but represented as a Character[] in Java rather than String.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class CharacterArrayClobType extends BasicTypeImpl<Character[]> {
	public static final CharacterArrayClobType INSTANCE = new CharacterArrayClobType();

	public CharacterArrayClobType() {
		super( CharacterArrayJavaDescriptor.INSTANCE, ClobTypeDescriptor.DEFAULT );
	}

	public String getName() {
		// todo name these annotation types for addition to the registry
		return null;
	}

	@Override
	public JdbcLiteralFormatter<Character[]> getJdbcLiteralFormatter() {
		// no support for CLOB literals
		return null;
	}

}
