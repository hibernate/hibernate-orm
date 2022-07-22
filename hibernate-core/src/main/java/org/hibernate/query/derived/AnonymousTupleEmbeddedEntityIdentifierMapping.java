/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.derived;

import java.util.Map;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.sqm.SqmExpressible;

/**
 * @author Christian Beikov
 */
@Incubating
public class AnonymousTupleEmbeddedEntityIdentifierMapping extends AnonymousTupleEmbeddableValuedModelPart
		implements CompositeIdentifierMapping, SingleAttributeIdentifierMapping {

	private final CompositeIdentifierMapping delegate;

	public AnonymousTupleEmbeddedEntityIdentifierMapping(
			Map<String, ModelPart> modelParts,
			DomainType<?> domainType,
			String componentName,
			CompositeIdentifierMapping delegate) {
		super(
				modelParts,
				domainType,
				componentName,
				(EmbeddableValuedModelPart) delegate
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
	public PropertyAccess getPropertyAccess() {
		return ((SingleAttributeIdentifierMapping) delegate).getPropertyAccess();
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
	public MappingType getMappedType() {
		return this;
	}

	@Override
	public EmbeddableMappingType getPartMappingType() {
		return (EmbeddableMappingType) super.getPartMappingType();
	}
}
