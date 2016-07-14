/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.common.internal;

import org.hibernate.persister.common.spi.AbstractAttributeImpl;
import org.hibernate.sqm.domain.ManagedType;
import org.hibernate.sqm.domain.SingularAttribute;
import org.hibernate.sqm.domain.Type;

/**
 * @author Steve Ebersole
 */
public class SingularAttributeImpl extends AbstractAttributeImpl implements SingularAttribute {
	private final Classification classification;
	private final Type type;

	private boolean isId;
	private boolean isVersion;

	public SingularAttributeImpl(
			ManagedType declaringType,
			String name,
			Classification classification,
			Type type) {
		super( declaringType, name );
		this.classification = classification;
		this.type = type;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public Type getBoundType() {
		return getType();
	}

	@Override
	public ManagedType asManagedType() {
		// todo : for now, just let the ClassCastException happen
		return (ManagedType) type;
	}

	@Override
	public Classification getAttributeTypeClassification() {
		return classification;
	}

	@Override
	public boolean isId() {
		return isId;
	}

	@Override
	public boolean isVersion() {
		return isVersion;
	}
}
