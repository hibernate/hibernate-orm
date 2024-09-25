/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.derived;

import java.util.List;
import java.util.Set;

import org.hibernate.Incubating;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.MergeContext;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.IdClassEmbeddable;
import org.hibernate.metamodel.mapping.internal.VirtualIdEmbeddable;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.sql.ast.spi.SqlSelection;

import jakarta.persistence.metamodel.Attribute;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Christian Beikov
 */
@Incubating
public class AnonymousTupleNonAggregatedEntityIdentifierMapping extends AnonymousTupleEmbeddableValuedModelPart
		implements NonAggregatedIdentifierMapping {

	private final NonAggregatedIdentifierMapping delegate;

	public AnonymousTupleNonAggregatedEntityIdentifierMapping(
			SqmExpressible<?> sqmExpressible,
			List<SqlSelection> sqlSelections,
			int selectionIndex,
			String selectionExpression,
			Set<String> compatibleTableExpressions,
			Set<Attribute<?, ?>> attributes,
			DomainType<?> domainType,
			String componentName,
			NonAggregatedIdentifierMapping delegate) {
		super(
				sqmExpressible,
				sqlSelections,
				selectionIndex,
				selectionExpression,
				compatibleTableExpressions,
				attributes,
				domainType,
				componentName,
				delegate,
				-1
		);
		this.delegate = delegate;
	}

	@Override
	public Nature getNature() {
		return Nature.VIRTUAL;
	}

	@Override
	public String getAttributeName() {
		return null;
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

	@Override
	public EmbeddableMappingType getPartMappingType() {
		return this;
	}

	@Override
	public VirtualIdEmbeddable getVirtualIdEmbeddable() {
		return delegate.getVirtualIdEmbeddable();
	}

	@Override
	public IdClassEmbeddable getIdClassEmbeddable() {
		return delegate.getIdClassEmbeddable();
	}

	@Override
	public IdentifierValueMapper getIdentifierValueMapper() {
		return delegate.getIdentifierValueMapper();
	}

	@Override
	public FetchStyle getStyle() {
		return FetchStyle.JOIN;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}

	@Override
	public boolean areEqual(@Nullable Object one, @Nullable Object other, SharedSessionContractImplementor session) {
		return delegate.areEqual( one, other, session );
	}
}
