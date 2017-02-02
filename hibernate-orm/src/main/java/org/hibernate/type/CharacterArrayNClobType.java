/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.CharacterArrayTypeDescriptor;
import org.hibernate.type.descriptor.sql.NClobTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#NCLOB NCLOB} and {@link Character Character[]}
 * <p/>
 * Essentially a {@link org.hibernate.type.MaterializedNClobType} but represented as a Character[] in Java rather than String.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public class CharacterArrayNClobType extends AbstractSingleColumnStandardBasicType<Character[]> {
	public static final CharacterArrayNClobType INSTANCE = new CharacterArrayNClobType();

	public CharacterArrayNClobType() {
		super( NClobTypeDescriptor.DEFAULT, CharacterArrayTypeDescriptor.INSTANCE );
	}

	public String getName() {
		// todo name these annotation types for addition to the registry
		return null;
	}

}
