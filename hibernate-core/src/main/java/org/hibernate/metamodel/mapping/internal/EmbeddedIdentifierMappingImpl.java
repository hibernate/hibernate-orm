/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.internal.AbstractCompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.Clause;
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
		implements SingleAttributeIdentifierMapping {
	private final String name;
	private final PropertyAccess propertyAccess;

	@SuppressWarnings("WeakerAccess")
	public EmbeddedIdentifierMappingImpl(
			EntityMappingType entityMapping,
			String name,
			EmbeddableMappingType embeddableDescriptor,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			PropertyAccess propertyAccess,
			String tableExpression,
			SessionFactoryImplementor sessionFactory) {
		super(
				attributeMetadataAccess,
				embeddableDescriptor,
				entityMapping,
				tableExpression,
				sessionFactory
		);

		this.name = name;
		this.propertyAccess = propertyAccess;
	}

	@Override
	public String getPartName() {
		return name;
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
	public Object getIdentifier(Object entity, SharedSessionContractImplementor session) {
		return getIdentifier( entity );
	}

	@Override
	public Object getIdentifier(Object entity, SessionFactoryImplementor sessionFactory) {
		return EmbeddedIdentifierMappingImpl.this.getIdentifier( entity );
	}

	private Object getIdentifier(Object entity) {
		if ( entity instanceof HibernateProxy ) {
			return ( (HibernateProxy) entity ).getHibernateLazyInitializer().getIdentifier();
		}
		return propertyAccess.getGetter().get( entity );
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		propertyAccess.getSetter().set( entity, id, session.getFactory() );
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
	public int getNumberOfFetchables() {
		return getEmbeddableTypeDescriptor().getNumberOfAttributeMappings();
	}


	@Override
	public int getAttributeCount() {
		return getEmbeddableTypeDescriptor().getNumberOfAttributeMappings();
	}

	@Override
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public List<SingularAttributeMapping> getAttributes() {
		return (List) getEmbeddableTypeDescriptor().getAttributeMappings();
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
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		getEmbeddableTypeDescriptor().breakDownJdbcValues( domainValue, valueConsumer, session );
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return getEmbeddableTypeDescriptor().forEachDisassembledJdbcValue(
				value,
				clause,
				offset,
				valuesConsumer,
				session
		);
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		final Object[] result = new Object[getAttributeCount()];
		forEachAttribute(
				(i, mapping) -> {
					Object o = mapping.getPropertyAccess().getGetter().get( value );
					result[i] = mapping.disassemble( o, session );
				}
		);
		return result;
	}
}
