/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.configuration.internal.metadata;

import org.hibernate.envers.boot.EnversMappingException;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Subclass;
import org.hibernate.mapping.UnionSubclass;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public enum InheritanceType {
	NONE,
	JOINED,
	SINGLE,
	TABLE_PER_CLASS;

	/**
	 * @param pc The class for which to get the inheritance type.
	 *
	 * @return The inheritance type of this class. NONE, if this class does not inherit from
	 *         another persistent class.
	 */
	public static InheritanceType get(PersistentClass pc) {
		final PersistentClass superclass = pc.getSuperclass();
		if ( superclass == null ) {
			return InheritanceType.NONE;
		}

		// We assume that every subclass is of the same type.
		final Subclass subclass = superclass.getSubclasses().get(0);
		if ( subclass instanceof SingleTableSubclass ) {
			return InheritanceType.SINGLE;
		}
		else if ( subclass instanceof JoinedSubclass ) {
			return InheritanceType.JOINED;
		}
		else if ( subclass instanceof UnionSubclass ) {
			return InheritanceType.TABLE_PER_CLASS;
		}

		throw new EnversMappingException( "Unknown subclass class: " + subclass.getClass() );
	}
}
