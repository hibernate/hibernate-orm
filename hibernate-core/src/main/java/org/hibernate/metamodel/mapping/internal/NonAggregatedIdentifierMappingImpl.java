/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.io.Serializable;
import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.internal.AbstractCompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.ComponentType;

/**
 * A "non-aggregated" composite identifier.
 * <p>
 * This is an identifier mapped using JPA's {@link jakarta.persistence.MapsId} feature.
 *
 * @author Steve Ebersole
 * @apiNote Technically a MapsId id does not have to be composite; we still handle that this class however
 */
public class NonAggregatedIdentifierMappingImpl extends AbstractCompositeIdentifierMapping {

	private final List<SingularAttributeMapping> idAttributeMappings;
	private final Component bootCidDescriptor;
	private final Component bootIdClassDescriptor;

	public NonAggregatedIdentifierMappingImpl(
			EmbeddableMappingType embeddableDescriptor,
			EntityMappingType entityMapping,
			List<SingularAttributeMapping> idAttributeMappings,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			String rootTableName,
			Component bootIdClassDescriptor,
			Component bootCidDescriptor,
			MappingModelCreationProcess creationProcess) {
		// todo (6.0) : handle MapsId
		super(
				attributeMetadataAccess,
				embeddableDescriptor,
				entityMapping,
				rootTableName,
				creationProcess.getCreationContext().getSessionFactory()
		);

		this.idAttributeMappings = idAttributeMappings;
		this.bootCidDescriptor = bootCidDescriptor;
		this.bootIdClassDescriptor = bootIdClassDescriptor;
	}

	@Override
	public int getAttributeCount() {
		return idAttributeMappings.size();
	}

	@Override
	public List<SingularAttributeMapping> getAttributes() {
		return idAttributeMappings;
	}

	@Override
	public Object getIdentifier(Object entity, SharedSessionContractImplementor session) {
		if ( hasContainingClass() ) {
			final Serializable disassemble = bootCidDescriptor.getType().disassemble( entity, session, null );
			return bootIdClassDescriptor.getType().assemble( disassemble, session, null );
		}
		else {
			return entity;
		}
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		final SessionFactoryImplementor factory = session.getFactory();
		final Object[] propertyValues = ( (ComponentType) bootIdClassDescriptor.getType() )
				.getPropertyValues( id, session );
		forEachAttribute(
				(position, attribute) -> {
					Object propertyValue = propertyValues[position];
					final Property property = bootIdClassDescriptor.getProperty( position );
					if ( attribute instanceof ToOneAttributeMapping && !( property.getValue() instanceof ManyToOne ) ) {
						final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) attribute;
						final EntityPersister entityPersister = toOneAttributeMapping.getEntityMappingType()
								.getEntityPersister();
						final EntityKey entityKey = session.generateEntityKey(
								propertyValue,
								entityPersister
						);
						final PersistenceContext persistenceContext = session.getPersistenceContext();
						// it is conceivable there is a proxy, so check that first
						propertyValue = persistenceContext.getProxy( entityKey );
						if ( propertyValue == null ) {
							// otherwise look for an initialized version
							propertyValue = persistenceContext.getEntity( entityKey );
							if ( propertyValue == null ) {
								// get the association out of the entity itself
								propertyValue = factory.getMetamodel()
										.findEntityDescriptor( entity.getClass() )
										.getPropertyValue( entity, toOneAttributeMapping.getAttributeName() );
							}
						}
					}
					attribute.getPropertyAccess()
							.getSetter()
							.set( entity, propertyValue, factory );
				}
		);
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		assert domainValue instanceof Object[];

		final Object[] values = (Object[]) domainValue;
		assert values.length == idAttributeMappings.size();

		for ( int i = 0; i < idAttributeMappings.size(); i++ ) {
			final SingularAttributeMapping attribute = idAttributeMappings.get( i );
			attribute.breakDownJdbcValues( values[ i ], valueConsumer, session );
		}
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		for ( int i = 0; i < idAttributeMappings.size(); i++ ) {
			idAttributeMappings.get( i ).applySqlSelections( navigablePath, tableGroup, creationState );
		}
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		for ( int i = 0; i < idAttributeMappings.size(); i++ ) {
			idAttributeMappings.get( i ).applySqlSelections(
					navigablePath,
					tableGroup,
					creationState,
					selectionConsumer
			);
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddableValuedFetchable

	@Override
	public String getSqlAliasStem() {
		return "id";
	}

	@Override
	public String getFetchableName() {
		return EntityIdentifierMapping.ROLE_LOCAL_NAME;
	}

	@Override
	public int getNumberOfFetchables() {
		return idAttributeMappings.size();
	}

	@Override
	public boolean hasContainingClass() {
		return bootIdClassDescriptor != bootCidDescriptor;
	}
}
