/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Collection;
import java.util.Iterator;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.internal.AbstractCompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.sql.ast.Clause;

/**
 * Support for {@link javax.persistence.EmbeddedId}
 *
 * @author Andrea Boriero
 */
public class EmbeddedIdentifierMappingImpl extends AbstractCompositeIdentifierMapping
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
			String[] attrColumnNames,
			SessionFactoryImplementor sessionFactory) {
		super(
				attributeMetadataAccess,
				embeddableDescriptor,
				entityMapping,
				tableExpression,
				attrColumnNames,
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
	public Object getIdentifier(Object entity, SharedSessionContractImplementor session) {
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
	public Collection<SingularAttributeMapping> getAttributes() {
		return (Collection) getEmbeddableTypeDescriptor().getAttributeMappings();
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
	public void visitDisassembledJdbcValues(
			Object value,
			Clause clause,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		getEmbeddableTypeDescriptor().visitDisassembledJdbcValues( value, clause, valuesConsumer, session );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		final Collection<SingularAttributeMapping> attributeMappings = getAttributes();

		Object[] result = new Object[attributeMappings.size()];
		int i = 0;
		final Iterator<SingularAttributeMapping> iterator = attributeMappings.iterator();
		while ( iterator.hasNext() ) {
			AttributeMapping mapping = iterator.next();
			Object o = mapping.getPropertyAccess().getGetter().get( value );
			result[i] = mapping.disassemble( o, session );
			i++;
		}
		return result;
	}
}
