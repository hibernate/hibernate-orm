/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tuple.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Set;

import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.MergeContext;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.mapping.SingleAttributeIdentifierMapping;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.sqm.spi.SqmExpressible;

import jakarta.persistence.metamodel.Attribute;

/**
 * @author Christian Beikov
 */
public class AnonymousTupleEmbeddedEntityIdentifierMapping extends AnonymousTupleEmbeddableValuedModelPart
		implements CompositeIdentifierMapping, SingleAttributeIdentifierMapping {

	private final CompositeIdentifierMapping delegate;

	public AnonymousTupleEmbeddedEntityIdentifierMapping(
			SqmExpressible<?> sqmExpressible,
			SqlTypedMapping[] sqlTypedMappings,
			int selectionIndex,
			String selectionExpression,
			Set<String> compatibleTableExpressions,
			Set<? extends Attribute<?, ?>> attributes,
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

	@Nonnull
	@Override
	public Nature getNature() {
		return delegate.getNature();
	}

	@Nonnull
	@Override
	public IdentifierValue getUnsavedStrategy() {
		return delegate.getUnsavedStrategy();
	}

	@Nullable
	@Override
	public Object getIdentifier(@Nonnull Object entity) {
		return delegate.getIdentifier( entity );
	}

	@Nullable
	@Override
	public Object getIdentifier(@Nonnull Object entity, @Nullable MergeContext mergeContext) {
		return delegate.getIdentifier( entity, mergeContext );
	}

	@Override
	public void setIdentifier(@Nonnull Object entity, @Nullable Object id, @Nonnull SharedSessionContractImplementor session) {
		delegate.setIdentifier( entity, id, session );
	}

	@Nullable
	@Override
	public Object instantiate() {
		return delegate.instantiate();
	}

	@Nonnull
	@Override
	public PropertyAccess getPropertyAccess() {
		return ((SingleAttributeIdentifierMapping) delegate).getPropertyAccess();
	}

	@Nonnull
	@Override
	public EmbeddableMappingType getPartMappingType() {
		return this;
	}

	@Override
	public int compare(@Nullable Object value1, @Nullable Object value2) {
		return super.compare( value1, value2 );
	}

	@Nonnull
	@Override
	public String getAttributeName() {
		return getPartName();
	}

	@Override
	public boolean hasContainingClass() {
		return true;
	}

	@Nonnull
	@Override
	public EmbeddableMappingType getMappedIdEmbeddableTypeDescriptor() {
		return this;
	}

	@Nonnull
	@Override
	public EmbeddableMappingType getMappedType() {
		return this;
	}

}
