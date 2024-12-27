/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tuple.internal;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.MergeContext;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.sqm.SqmExpressible;

/**
 * @author Christian Beikov
 */
@Incubating
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

	@Override
	public Nature getNature() {
		return Nature.SIMPLE;
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
	public Object getIdentifier(Object entity, MergeContext mergeContext) {
		return delegate.getIdentifier( entity, mergeContext );
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
		return delegate.getPropertyAccess();
	}

	@Override
	public String getAttributeName() {
		return getPartName();
	}

	@Override
	public ManagedMappingType getDeclaringType() {
		return (ManagedMappingType) super.getDeclaringType();
	}
}
