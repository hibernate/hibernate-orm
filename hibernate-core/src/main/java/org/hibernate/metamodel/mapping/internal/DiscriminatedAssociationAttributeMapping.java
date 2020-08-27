/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.Iterator;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.type.AnyType;
import org.hibernate.type.BasicType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.MetaType;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

import static org.hibernate.metamodel.mapping.internal.DiscriminatedAssociationMapping.KEY_ROLE_NAME;

/**
 * Singular, any-valued attribute
 *
 * @see org.hibernate.annotations.Any
 *
 * @author Steve Ebersole
 */
public class DiscriminatedAssociationAttributeMapping
		extends AbstractSingularAttributeMapping
		implements DiscriminatedAssociationModelPart {
	private final NavigableRole navigableRole;
	private final DiscriminatedAssociationMapping discriminatorMapping;

	public DiscriminatedAssociationAttributeMapping(
			NavigableRole attributeRole,
			JavaTypeDescriptor<Object> baseAssociationJtd,
			ManagedMappingType declaringType,
			int stateArrayPosition,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			FetchStrategy mappedFetchStrategy,
			PropertyAccess propertyAccess,
			Property bootProperty,
			AnyType anyType,
			Any bootValueMapping,
			MappingModelCreationProcess creationProcess) {
		super( bootProperty.getName(), stateArrayPosition, attributeMetadataAccess, mappedFetchStrategy, declaringType, propertyAccess );
		this.navigableRole = attributeRole;

		final SessionFactoryImplementor sessionFactory = creationProcess.getCreationContext().getSessionFactory();

		final JdbcEnvironment jdbcEnvironment = sessionFactory.getJdbcServices().getJdbcEnvironment();
		final String tableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				bootValueMapping.getTable().getQualifiedTableName(),
				jdbcEnvironment.getDialect()
		);

		assert bootValueMapping.getColumnSpan() > 1;
		final Iterator<Selectable> columnIterator = bootValueMapping.getColumnIterator();

		assert columnIterator.hasNext();

		final AnyDiscriminatorPart discriminatorPart = new AnyDiscriminatorPart(
				attributeRole,
				this,
				tableName,
				columnIterator.next().getText( jdbcEnvironment.getDialect() ),
				(MetaType) anyType.getDiscriminatorType()
		);


		final Fetchable keyPart;
		final Type keyType = anyType.getIdentifierType();
		if ( keyType instanceof BasicType ) {
			assert columnIterator.hasNext();

			keyPart = new AnyKeyPart(
					attributeRole.append( KEY_ROLE_NAME ),
					tableName,
					columnIterator.next().getText( jdbcEnvironment.getDialect() ),
					this,
					attributeMetadataAccess.resolveAttributeMetadata( null ).isNullable(),
					(BasicType<?>) keyType
			);

			assert ! columnIterator.hasNext();
		}
		else {
			assert keyType instanceof CompositeType;
			throw new NotYetImplementedFor6Exception( getClass() );
		}

		this.discriminatorMapping = new DiscriminatedAssociationMapping(
				this,
				discriminatorPart,
				keyPart,
				baseAssociationJtd,
				bootProperty.isLazy()
						? FetchTiming.DELAYED
						: FetchTiming.IMMEDIATE,
				sessionFactory
		);
	}

	@Override
	public BasicValuedModelPart getDiscriminatorPart() {
		return discriminatorMapping.getDiscriminatorPart();
	}

	@Override
	public ModelPart getKeyPart() {
		return discriminatorMapping.getKeyPart();
	}

	@Override
	public EntityMappingType resolveDiscriminatorValue(Object discriminatorValue) {
		return discriminatorMapping.resolveDiscriminatorValueToEntityName( discriminatorValue );
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		return discriminatorMapping.generateFetch(
				fetchParent,
				fetchablePath,
				fetchTiming,
				selected,
				lockMode,
				resultVariable,
				creationState
		);
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public MappingType getMappedType() {
		return discriminatorMapping;
	}
}
