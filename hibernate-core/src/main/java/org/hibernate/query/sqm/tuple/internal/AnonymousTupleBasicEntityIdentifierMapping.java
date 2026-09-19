/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tuple.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.MergeContext;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.sqm.spi.SqmExpressible;

/**
 * @author Christian Beikov
 */
public class AnonymousTupleBasicEntityIdentifierMapping
		extends AnonymousTupleBasicValuedModelPart
		implements BasicEntityIdentifierMapping {

	private final BasicEntityIdentifierMapping delegate;

	public AnonymousTupleBasicEntityIdentifierMapping(
			MappingType declaringType,
			String selectionExpression,
			SqmExpressible<?> expressible,
			JdbcMapping jdbcMapping,
			BasicEntityIdentifierMapping delegate) {
		super( declaringType, delegate.getAttributeName(), selectionExpression, expressible, jdbcMapping, -1 );
		this.delegate = delegate;
	}

	public AnonymousTupleBasicEntityIdentifierMapping(
			MappingType declaringType,
			SelectableMapping selectableMapping,
			SqmExpressible<?> expressible,
			BasicEntityIdentifierMapping delegate) {
		super( declaringType, delegate.getAttributeName(), selectableMapping, expressible, -1 );
		this.delegate = delegate;
	}

	@Nonnull
	@Override
	public Nature getNature() {
		return Nature.SIMPLE;
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
		return delegate.getPropertyAccess();
	}

	@Nonnull
	@Override
	public String getAttributeName() {
		return getPartName();
	}

	@Nonnull
	@Override
	public ManagedMappingType getDeclaringType() {
		return (ManagedMappingType) super.getDeclaringType();
	}
}
