/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.derived;

import java.util.Set;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.sqm.SqmExpressible;

import jakarta.persistence.metamodel.Attribute;

/**
 * @author Christian Beikov
 */
@Incubating
public class AnonymousTupleEmbeddedEntityIdentifierMapping extends AnonymousTupleEmbeddableValuedModelPart
		implements CompositeIdentifierMapping, SingleAttributeIdentifierMapping {

	private final CompositeIdentifierMapping delegate;

	public AnonymousTupleEmbeddedEntityIdentifierMapping(
			SqmExpressible<?> sqmExpressible,
			SqlTypedMapping[] sqlTypedMappings,
			int selectionIndex,
			String selectionExpression,
			Set<String> compatibleTableExpressions,
			Set<Attribute<?, ?>> attributes,
			DomainType<?> domainType,
			CompositeIdentifierMapping delegate) {
		super(
				sqmExpressible,
				sqlTypedMappings,
				selectionIndex,
				selectionExpression,
				compatibleTableExpressions,
				attributes,
				domainType,
				delegate.getAttributeName(),
				delegate,
				-1
		);
		this.delegate = delegate;
	}

	@Override
	public Nature getNature() {
		return delegate.getNature();
	}

	@Override
	public IdentifierValue getUnsavedStrategy() {
		return delegate.getUnsavedStrategy();
	}

	@Override
	public Object getIdentifier(Object entity) {
		return delegate.getIdentifier( entity );
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		delegate.setIdentifier( entity, id, session );
	}

	@Override
	public Object instantiate() {
		return delegate.instantiate();
	}

	@Override
	public PropertyAccess getPropertyAccess() {
		return ((SingleAttributeIdentifierMapping) delegate).getPropertyAccess();
	}

	@Override
	public EmbeddableMappingType getPartMappingType() {
		return this;
	}

	@Override
	public int compare(Object value1, Object value2) {
		return super.compare( value1, value2 );
	}

	@Override
	public String getAttributeName() {
		return getPartName();
	}

	@Override
	public boolean hasContainingClass() {
		return true;
	}

	@Override
	public EmbeddableMappingType getMappedIdEmbeddableTypeDescriptor() {
		return this;
	}

	@Override
	public EmbeddableMappingType getMappedType() {
		return this;
	}

}
