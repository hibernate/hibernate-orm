package org.hibernate.query.sqm.tuple.internal;

import jakarta.annotation.Nonnull;

import java.util.Set;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.MergeContext;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.mapping.internal.IdClassEmbeddable;
import org.hibernate.metamodel.mapping.internal.VirtualIdEmbeddable;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.sqm.spi.SqmExpressible;

import jakarta.persistence.metamodel.Attribute;
import jakarta.annotation.Nullable;

/**
 * @author Christian Beikov
 */
public class AnonymousTupleNonAggregatedEntityIdentifierMapping extends AnonymousTupleEmbeddableValuedModelPart
		implements NonAggregatedIdentifierMapping {

	private final NonAggregatedIdentifierMapping delegate;

	public AnonymousTupleNonAggregatedEntityIdentifierMapping(
			SqmExpressible<?> sqmExpressible,
			SqlTypedMapping[] sqlTypedMappings,
			int selectionIndex,
			String selectionExpression,
			Set<String> compatibleTableExpressions,
			Set<? extends Attribute<?, ?>> attributes,
			DomainType<?> domainType,
			String componentName,
			NonAggregatedIdentifierMapping delegate) {
		super(
				sqmExpressible,
				sqlTypedMappings,
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

	@Nonnull
	@Override
	public Nature getNature() {
		return Nature.VIRTUAL;
	}

	@Nullable
	@Override
	public String getAttributeName() {
		return null;
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

	@Nonnull
	@Override
	public EmbeddableMappingType getPartMappingType() {
		return this;
	}

	@Nonnull
	@Override
	public VirtualIdEmbeddable getVirtualIdEmbeddable() {
		return delegate.getVirtualIdEmbeddable();
	}

	@Nullable
	@Override
	public IdClassEmbeddable getIdClassEmbeddable() {
		return delegate.getIdClassEmbeddable();
	}

	@Nonnull
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
	public boolean areEqual(@Nullable Object one, @Nullable Object other, @Nullable SharedSessionContractImplementor session) {
		return delegate.areEqual( one, other, session );
	}
}
