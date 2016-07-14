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

/**
 * @author Steve Ebersole
 */
public class IdentifierSimple
		implements IdentifierDescriptorImplementor, SingularAttributeImplementor, IdentifierDescriptorSingleAttribute {
	private final ManagedType declaringType;
	private final String attributeName;
	private final org.hibernate.type.BasicType ormType;
	private final org.hibernate.sqm.domain.BasicType sqmType;
	private final Column[] columns;

	public IdentifierSimple(
			ManagedType declaringType,
			String attributeName,
			org.hibernate.type.BasicType ormType,
			org.hibernate.sqm.domain.BasicType sqmType,
			Column[] columns) {
		this.declaringType = declaringType;
		this.attributeName = attributeName;
		this.ormType = ormType;
		this.sqmType = sqmType;
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
	public org.hibernate.type.BasicType getOrmType() {
		return ormType;
	}

	@Override
	public org.hibernate.sqm.domain.BasicType getIdType() {
		return sqmType;
	}

	@Override
	public org.hibernate.sqm.domain.BasicType getType() {
		return sqmType;
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
	public org.hibernate.sqm.domain.BasicType getBoundType() {
		return sqmType;
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
