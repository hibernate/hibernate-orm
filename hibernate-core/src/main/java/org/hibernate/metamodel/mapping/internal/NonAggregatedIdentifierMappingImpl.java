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

import org.hibernate.EntityNameResolver;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.metamodel.internal.AbstractCompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.ComponentType;
import org.hibernate.type.Type;

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
	private final ComponentType mappedIdComponentType;
	private final ComponentType virtualComponentType;

	private final boolean[] isBootIdPropertyManyToOne;

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
		final int propertySpan = bootIdClassDescriptor.getPropertySpan();
		isBootIdPropertyManyToOne = new boolean[propertySpan];
		for ( int i = 0; i < propertySpan; i++ ) {
			isBootIdPropertyManyToOne[i] = bootIdClassDescriptor.getProperty( i ).getValue() instanceof ManyToOne;
		}
		mappedIdComponentType = (ComponentType) bootIdClassDescriptor.getType();
		virtualComponentType = (ComponentType) bootCidDescriptor.getType();
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
			final Serializable disassemble = virtualComponentType.disassemble( entity, session, null );
			return mappedIdComponentType.assemble( disassemble, session, null );
		}
		else {
			return entity;
		}
	}

	@Override
	public Object getIdentifier(Object entity, SessionFactoryImplementor sessionFactory){
		if ( hasContainingClass() ) {
			final Object id = mappedIdComponentType.instantiate();
			final Object[] propertyValues = virtualComponentType.getPropertyValues( entity );
			final Type[] subTypes = virtualComponentType.getSubtypes();
			final Type[] copierSubTypes = mappedIdComponentType.getSubtypes();
			final int length = subTypes.length;
			for ( int i = 0; i < length; i++ ) {
				//JPA 2 @MapsId + @IdClass points to the pk of the entity
				if ( subTypes[i].isAssociationType() && !copierSubTypes[i].isAssociationType()  ) {
					propertyValues[i] = determineEntityId( propertyValues[i], sessionFactory );
				}
			}
			mappedIdComponentType.setPropertyValues( id, propertyValues );
			return id;
		}
		else {
			return entity;
		}
	}

	private static Object determineEntityId(Object entity, SessionFactoryImplementor sessionFactory) {
		if ( entity == null ) {
			return null;
		}

		if ( HibernateProxy.class.isInstance( entity ) ) {
			// entity is a proxy, so we know it is not transient; just return ID from proxy
			return ( (HibernateProxy) entity ).getHibernateLazyInitializer().getInternalIdentifier();
		}

		final EntityPersister persister = resolveEntityPersister(
				entity,
				sessionFactory
		);

		return persister.getIdentifier( entity, sessionFactory );
	}

	private static EntityPersister resolveEntityPersister(Object entity, SessionFactoryImplementor sessionFactory) {
		assert sessionFactory != null;

		String entityName = null;
		final MetamodelImplementor metamodel = sessionFactory.getMetamodel();
		for ( EntityNameResolver entityNameResolver : metamodel.getEntityNameResolvers() ) {
			entityName = entityNameResolver.resolveEntityName( entity );
			if ( entityName != null ) {
				break;
			}
		}
		if ( entityName == null ) {
			// old fall-back
			entityName = entity.getClass().getName();
		}

		return metamodel.findEntityDescriptor( entityName );
	}



	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		if ( !getEmbeddableTypeDescriptor().getMappedJavaTypeDescriptor()
				.getJavaTypeClass()
				.isAssignableFrom( value.getClass() ) ) {
			final Object[] result = new Object[idAttributeMappings.size()];
			for ( int i = 0; i < idAttributeMappings.size(); i++ ) {
				final AttributeMapping attributeMapping = idAttributeMappings.get( i );
				Object o = attributeMapping.getPropertyAccess().getGetter().get( value );
				result[i] = attributeMapping.disassemble( o, session );
			}

			return result;
		}

		return getEmbeddableTypeDescriptor().disassemble( value, session );
	}

	@Override
	public void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session) {
		if ( !getEmbeddableTypeDescriptor().getMappedJavaTypeDescriptor().getJavaTypeClass().isAssignableFrom( id.getClass()) ) {
			idAttributeMappings.get( 0 ).getPropertyAccess().getSetter().set( entity, id, session.getFactory() );
		}
		else {
			final SessionFactoryImplementor factory = session.getFactory();
			final Object[] propertyValues = mappedIdComponentType.getPropertyValues( id, session );
			forEachAttribute(
					(position, attribute) -> {
						Object propertyValue = propertyValues[position];
						if ( attribute instanceof ToOneAttributeMapping && !isBootIdPropertyManyToOne[position] ) {
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
		return "id";
	}

	@Override
	public int getNumberOfFetchables() {
		return idAttributeMappings.size();
	}

	@Override
	public boolean hasContainingClass() {
		return mappedIdComponentType != virtualComponentType;
	}

//	@Override
//	public int forEachJdbcValue(
//			Object value,
//			Clause clause,
//			int offset,
//			JdbcValuesConsumer consumer,
//			SharedSessionContractImplementor session) {
//
//		final List<AttributeMapping> attributeMappings = getEmbeddableTypeDescriptor().getAttributeMappings();
//		int span = 0;
//
//		for ( int i = 0; i < attributeMappings.size(); i++ ) {
//			final AttributeMapping attributeMapping = attributeMappings.get( i );
//			if ( attributeMapping instanceof PluralAttributeMapping ) {
//				continue;
//			}
//			final Object o = attributeMapping.getPropertyAccess().getGetter().get( value );
//			span += attributeMapping.forEachJdbcValue( o, clause, span + offset, consumer, session );
//		}
//		return span;
//	}
}
