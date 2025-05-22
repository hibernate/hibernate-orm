/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiConsumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.internal.AbstractCompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.AggregatedIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResultCreationState;

/**
 * Support for {@link jakarta.persistence.EmbeddedId}
 *
 * @author Andrea Boriero
 */
public class EmbeddedIdentifierMappingImpl
		extends AbstractCompositeIdentifierMapping
		implements AggregatedIdentifierMapping {
	private final String name;
	private final EmbeddableMappingType embeddableDescriptor;
	private final PropertyAccess propertyAccess;

	public EmbeddedIdentifierMappingImpl(
			EntityMappingType entityMapping,
			String name,
			EmbeddableMappingType embeddableDescriptor,
			PropertyAccess propertyAccess,
			String tableExpression,
			MappingModelCreationProcess creationProcess) {
		super( entityMapping, tableExpression, creationProcess );

		this.name = name;
		this.embeddableDescriptor = embeddableDescriptor;
		this.propertyAccess = propertyAccess;
	}

	@Override
	public String getPartName() {
		return name;
	}

	@Override
	public Nature getNature() {
		return Nature.COMPOSITE;
	}

	@Override
	public EmbeddableMappingType getPartMappingType() {
		return embeddableDescriptor;
	}

	@Override
	public EmbeddableMappingType getMappedIdEmbeddableTypeDescriptor() {
		return getMappedType();
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState) {
		getEmbeddableTypeDescriptor().applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		getEmbeddableTypeDescriptor().applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	@Override
	public Object getIdentifier(Object entity) {
		final LazyInitializer lazyInitializer = HibernateProxy.extractLazyInitializer( entity );
		if ( lazyInitializer != null ) {
			return lazyInitializer.getInternalIdentifier();
		}
		return propertyAccess.getGetter().get( entity );
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		propertyAccess.getSetter().set( entity, id );
	}

	@Override
	public String getSqlAliasStem() {
		return name;
	}


	@Override
	public String getFetchableName() {
		return name;
	}

	@Override
	public PropertyAccess getPropertyAccess() {
		return propertyAccess;
	}

	@Override
	public String getAttributeName() {
		return name;
	}

	@Override
	public int compare(Object value1, Object value2) {
		return getEmbeddableTypeDescriptor().compare( value1, value2 );
	}
}
