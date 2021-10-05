/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.sql.Types;

import org.hibernate.type.descriptor.java.CharacterArrayJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.ClobJdbcTypeDescriptor;

/**
 * A type that maps between {@link Types#CLOB CLOB} and {@link Character Character[]}
 * <p/>
 * Essentially a {@link MaterializedClobType} but represented as a Character[] in Java rather than String.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class CharacterArrayClobType
		extends AbstractSingleColumnStandardBasicType<Character[]>
		implements AdjustableBasicType<Character[]> {
	public static final CharacterArrayClobType INSTANCE = new CharacterArrayClobType();

	public CharacterArrayClobType() {
		super( ClobJdbcTypeDescriptor.DEFAULT, CharacterArrayJavaTypeDescriptor.INSTANCE );
	}

	public String getName() {
		// todo name these annotation types for addition to the registry
		return null;
	}
}
