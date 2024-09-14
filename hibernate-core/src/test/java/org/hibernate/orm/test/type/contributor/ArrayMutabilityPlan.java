/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type.contributor;

import java.io.Serializable;

import org.hibernate.SharedSessionContract;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/**
 * @author Steve Ebersole
 */
public class ArrayMutabilityPlan implements MutabilityPlan<Array> {
	/**
	 * Singleton access
	 */
	public static final ArrayMutabilityPlan INSTANCE = new ArrayMutabilityPlan();

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public Array deepCopy(Array value) {
		if ( value == null ) {
			return null;
		}

		final Array copy = new Array();
		copy.addAll( value );
		return copy;
	}

	@Override
	public Serializable disassemble(Array value, SharedSessionContract session) {
		return ArrayJavaType.INSTANCE.toString( value );
	}

	@Override
	public Array assemble(Serializable cached, SharedSessionContract session) {
		return ArrayJavaType.INSTANCE.fromString( (String) cached );
	}
}
