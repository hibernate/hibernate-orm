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
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.MergeContext;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.sql.ast.spi.SqlSelection;

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
			List<SqlSelection> sqlSelections,
			int selectionIndex,
			String selectionExpression,
			Set<String> compatibleTableExpressions,
			Set<Attribute<?, ?>> attributes,
			DomainType<?> domainType,
			CompositeIdentifierMapping delegate) {
		super(
				sqmExpressible,
				sqlSelections,
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
