/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.derived;

import java.util.Map;

import org.hibernate.Incubating;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NonAggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.IdClassEmbeddable;
import org.hibernate.metamodel.mapping.internal.VirtualIdEmbeddable;
import org.hibernate.metamodel.model.domain.DomainType;

/**
 * @author Christian Beikov
 */
@Incubating
public class AnonymousTupleNonAggregatedEntityIdentifierMapping extends AnonymousTupleEmbeddableValuedModelPart
		implements NonAggregatedIdentifierMapping {

	private final NonAggregatedIdentifierMapping delegate;

	public AnonymousTupleNonAggregatedEntityIdentifierMapping(
			Map<String, ModelPart> modelParts,
			DomainType<?> domainType,
			String componentName,
			NonAggregatedIdentifierMapping delegate) {
		super(
				modelParts,
				domainType,
				componentName,
				delegate
		);
		this.delegate = delegate;
	}

	@Override
	public IdentifierValue getUnsavedStrategy() {
		return delegate.getUnsavedStrategy();
	}

	@Override
	public Object getIdentifier(Object entity, SharedSessionContractImplementor session) {
		return delegate.getIdentifier( entity, session );
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
	public boolean hasContainingClass() {
		return true;
	}

	@Override
	public EmbeddableMappingType getMappedIdEmbeddableTypeDescriptor() {
		return this;
	}

	@Override
	public MappingType getMappedType() {
		return this;
	}

	@Override
	public EmbeddableMappingType getPartMappingType() {
		return (EmbeddableMappingType) super.getPartMappingType();
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
}
