/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity.spi;

import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.SingularAttributeImplementor;
import org.hibernate.sqm.domain.IdentifierDescriptorSingleAttribute;
import org.hibernate.sqm.domain.ManagedType;
import org.hibernate.sqm.domain.SingularAttribute;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class EntityIdentifierSimple
		implements EntityIdentifier, SingularAttributeImplementor, IdentifierDescriptorSingleAttribute {
	private final ManagedType declaringType;
	private final String attributeName;
	private final BasicType type;
	private final Column[] columns;

	public EntityIdentifierSimple(
			ManagedType declaringType,
			String attributeName,
			BasicType type,
			Column[] columns) {
		this.declaringType = declaringType;
		this.attributeName = attributeName;
		this.type = type;
		this.columns = columns;
	}

	@Override
	public ManagedType getDeclaringType() {
		return declaringType;
	}

	@Override
	public String getName() {
		return attributeName;
	}

	@Override
	public Classification getAttributeTypeClassification() {
		return Classification.BASIC;
	}

	@Override
	public BasicType getIdType() {
		return type;
	}

	@Override
	public BasicType getType() {
		return getIdType();
	}

	@Override
	public boolean hasSingleIdAttribute() {
		return true;
	}

	@Override
	public String getReferableAttributeName() {
		return attributeName;
	}

	@Override
	public Column[] getColumns() {
		return columns;
	}

	@Override
	public boolean isId() {
		return true;
	}

	@Override
	public boolean isVersion() {
		return false;
	}

	@Override
	public BasicType getBoundType() {
		return getIdType();
	}

	@Override
	public ManagedType asManagedType() {
		return null;
	}

	@Override
	public SingularAttribute getIdAttribute() {
		return this;
	}
}
