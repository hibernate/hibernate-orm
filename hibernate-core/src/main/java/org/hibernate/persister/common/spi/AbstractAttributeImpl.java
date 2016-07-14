/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.common.spi;

import org.hibernate.sqm.domain.ManagedType;

/**
 * Base class for Attribute implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractAttributeImpl implements AttributeImplementor {
	private final ManagedType declaringType;
	private final String name;

	public AbstractAttributeImpl(ManagedType declaringType, String name) {
		this.declaringType = declaringType;
		this.name = name;
	}

	@Override
	public ManagedType getDeclaringType() {
		return declaringType;
	}

	@Override
	public String getName() {
		return name;
	}
}
