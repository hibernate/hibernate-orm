/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.MutableInteger;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.internal.AbstractCompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.ComponentType;

/**
 * A "non-aggregated" composite identifier.
 * <p>
 * This is an identifier mapped using JPA's {@link javax.persistence.MapsId} feature.
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
			String[] rootTableKeyColumnNames,
			Component bootCidDescriptor,
			Component bootIdClassDescriptor,
			MappingModelCreationProcess creationProcess) {
		// todo (6.0) : handle MapsId
		super(
				attributeMetadataAccess,
				embeddableDescriptor,
				entityMapping,
				rootTableName,
				rootTableKeyColumnNames,
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
	public Collection<SingularAttributeMapping> getAttributes() {
		return idAttributeMappings;
	}

	@Override
	public Object getIdentifier(Object entity, SharedSessionContractImplementor session) {
		if ( entity instanceof HibernateProxy ) {
			return ( (HibernateProxy) entity ).getHibernateLazyInitializer().getIdentifier();
		}
		final Serializable disassemble = bootCidDescriptor.getType().disassemble( entity, session, null );
		return bootIdClassDescriptor.getType().assemble( disassemble, session, null );
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		final SessionFactoryImplementor factory = session.getFactory();
		final Object[] propertyValues = ( (ComponentType) bootIdClassDescriptor.getType() )
				.getPropertyValues( id, session );
		final MutableInteger index = new MutableInteger();
		getAttributes().forEach(
				attribute -> {
					final int position = index.getAndIncrement();
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

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// EmbeddableValuedFetchable

	@Override
	public String getSqlAliasStem() {
		return "id";
	}

	@Override
	public String getFetchableName() {
		return "id";
	}

	@Override
	public int getNumberOfFetchables() {
		return idAttributeMappings.size();
	}

}
