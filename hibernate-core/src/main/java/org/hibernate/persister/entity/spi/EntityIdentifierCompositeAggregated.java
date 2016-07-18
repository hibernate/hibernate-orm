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
import org.hibernate.type.spi.CompositeType;

/**
 * @author Steve Ebersole
 */
public class EntityIdentifierCompositeAggregated
		implements EntityIdentifier, SingularAttributeImplementor, IdentifierDescriptorSingleAttribute {
	private final ManagedType declaringType;
	private final String attributeName;
	private final CompositeType compositeType;

	public EntityIdentifierCompositeAggregated(
			ManagedType declaringType,
			String attributeName,
			CompositeType compositeType) {
		this.declaringType = declaringType;
		this.attributeName = attributeName;
		this.compositeType = compositeType;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// IdentifierDescriptor

	@Override
	public SingularAttribute getIdAttribute() {
		return this;
	}

	@Override
	public CompositeType getIdType() {
		return compositeType;
	}

	@Override
	public boolean hasSingleIdAttribute() {
		return true;
	}

	@Override
	public String getReferableAttributeName() {
		return attributeName;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SingularAttribute

	@Override
	public Classification getAttributeTypeClassification() {
		return Classification.EMBEDDED;
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
	public Column[] getColumns() {
		return compositeType.getEmbeddablePersister().collectColumns();
	}

	@Override
	public CompositeType getType() {
		return compositeType;
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
	public org.hibernate.sqm.domain.Type getBoundType() {
		return compositeType.getEmbeddablePersister().getBoundType();
	}

	@Override
	public ManagedType asManagedType() {
		return compositeType.getEmbeddablePersister().asManagedType();
	}
}
