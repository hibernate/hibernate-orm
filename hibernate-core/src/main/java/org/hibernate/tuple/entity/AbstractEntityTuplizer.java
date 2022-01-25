/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple.entity;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.bytecode.spi.BytecodeEnhancementMetadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.ComponentType;


/**
 * Support for tuplizers relating to entities.
 *
 * @author Steve Ebersole
 * @author Gavin King
 *
 * @deprecated like its supertypes
 */
@Deprecated(since = "6.0")
public abstract class AbstractEntityTuplizer implements EntityTuplizer {

	private final EntityMetamodel entityMetamodel;

	protected final Getter[] getters;
	protected final Setter[] setters;
	protected final int propertySpan;
	protected final boolean hasCustomAccessors;

	private EntityPersister entityDescriptor;
	private EntityIdentifierMapping identifierMapping;

	/**
	 * Build an appropriate Getter for the given property.
	 *
	 * @param mappedProperty The property to be accessed via the built Getter.
	 * @param mappedEntity The entity information regarding the mapped entity owning this property.
	 *
	 * @return An appropriate Getter instance.
	 */
	protected abstract Getter buildPropertyGetter(Property mappedProperty, PersistentClass mappedEntity);

	/**
	 * Build an appropriate Setter for the given property.
	 *
	 * @param mappedProperty The property to be accessed via the built Setter.
	 * @param mappedEntity The entity information regarding the mapped entity owning this property.
	 *
	 * @return An appropriate Setter instance.
	 */
	protected abstract Setter buildPropertySetter(Property mappedProperty, PersistentClass mappedEntity);

	/**
	 * Build an appropriate ProxyFactory for the given mapped entity.
	 *
	 * @param mappingInfo The mapping information regarding the mapped entity.
	 * @param idGetter The constructed Getter relating to the entity's id property.
	 * @param idSetter The constructed Setter relating to the entity's id property.
	 *
	 * @return An appropriate ProxyFactory instance.
	 */
	protected abstract ProxyFactory buildProxyFactory(PersistentClass mappingInfo, Getter idGetter, Setter idSetter);

	/**
	 * Constructs a new AbstractEntityTuplizer instance.
	 *
	 * @param entityMetamodel The "interpreted" information relating to the mapped entity.
	 * @param mappingInfo The parsed "raw" mapping data relating to the given entity.
	 */
	public AbstractEntityTuplizer(EntityMetamodel entityMetamodel, PersistentClass mappingInfo) {
		this.entityMetamodel = entityMetamodel;
		propertySpan = entityMetamodel.getPropertySpan();

		getters = new Getter[propertySpan];
		setters = new Setter[propertySpan];

		final Iterator<Property> itr = mappingInfo.getPropertyClosureIterator();
		boolean foundCustomAccessor = false;
		int i = 0;
		while ( itr.hasNext() ) {
			//TODO: redesign how PropertyAccessors are acquired...
			Property property = itr.next();
			getters[i] = buildPropertyGetter( property, mappingInfo );
			setters[i] = buildPropertySetter( property, mappingInfo );
			if ( !property.isBasicPropertyAccessor() ) {
				foundCustomAccessor = true;
			}
			i++;
		}
		hasCustomAccessors = foundCustomAccessor;
	}

	/**
	 * Retrieves the defined entity-name for the tuplized entity.
	 *
	 * @return The entity-name.
	 */
	protected String getEntityName() {
		return entityMetamodel.getName();
	}

	/**
	 * Retrieves the defined entity-names for any subclasses defined for this
	 * entity.
	 *
	 * @return Any subclass entity-names.
	 */
	protected Set<String> getSubclassEntityNames() {
		return entityMetamodel.getSubclassEntityNames();
	}


	@Override
	public Object getIdentifier(Object entity, SharedSessionContractImplementor session) {
		final EntityIdentifierMapping identifierMapping = resolveIdentifierDescriptor();
		return identifierMapping.getIdentifier( entity, session );
	}

	protected EntityIdentifierMapping resolveIdentifierDescriptor() {
		if ( identifierMapping == null ) {
			identifierMapping = resolveEntityDescriptor().getIdentifierMapping();
		}

		return identifierMapping;
	}

	private EntityPersister resolveEntityDescriptor() {
		if ( entityDescriptor == null ) {
			entityDescriptor = getFactory()
					.getRuntimeMetamodels()
					.getMappingMetamodel()
					.findEntityDescriptor( getEntityName() );
		}

		return entityDescriptor;
	}


	protected boolean shouldGetAllProperties(Object entity) {
		final BytecodeEnhancementMetadata bytecodeEnhancementMetadata = getEntityMetamodel().getBytecodeEnhancementMetadata();
		if ( !bytecodeEnhancementMetadata.isEnhancedForLazyLoading() ) {
			return true;
		}

		return !bytecodeEnhancementMetadata.hasUnFetchedAttributes( entity );
	}

	@Override
	public Object[] getPropertyValuesToInsert(Object entity, Map mergeMap, SharedSessionContractImplementor session) {
		final EntityPersister entityDescriptor = resolveEntityDescriptor();
		return entityDescriptor.getPropertyValuesToInsert( entity, mergeMap, session );
	}

	@Override
	public Object getPropertyValue(Object entity, int i) throws HibernateException {
		final EntityPersister entityDescriptor = resolveEntityDescriptor();
		return entityDescriptor.getPropertyValue( entity, i );
	}

	/**
	 * Extract a component property value.
	 *
	 * @param type The component property types.
	 * @param component The component instance itself.
	 * @param propertyPath The property path for the property to be extracted.
	 *
	 * @return The property value extracted.
	 */
	protected Object getComponentValue(ComponentType type, Object component, String propertyPath) {
		final int loc = propertyPath.indexOf( '.' );
		final String basePropertyName = loc > 0
				? propertyPath.substring( 0, loc )
				: propertyPath;
		final int index = findSubPropertyIndex( type, basePropertyName );
		final Object baseValue = type.getPropertyValue( component, index );
		if ( loc > 0 ) {
			if ( baseValue == null ) {
				return null;
			}
			return getComponentValue(
					(ComponentType) type.getSubtypes()[index],
					baseValue,
					propertyPath.substring( loc + 1 )
			);
		}
		else {
			return baseValue;
		}

	}

	private int findSubPropertyIndex(ComponentType type, String subPropertyName) {
		final String[] propertyNames = type.getPropertyNames();
		for ( int index = 0; index < propertyNames.length; index++ ) {
			if ( subPropertyName.equals( propertyNames[index] ) ) {
				return index;
			}
		}
		throw new MappingException( "component property not found: " + subPropertyName );
	}

	protected final EntityMetamodel getEntityMetamodel() {
		return entityMetamodel;
	}

	protected final SessionFactoryImplementor getFactory() {
		return entityMetamodel.getSessionFactory();
	}

	@Override
	public String toString() {
		return getClass().getName() + '(' + getEntityMetamodel().getName() + ')';
	}

	@Override
	public Getter getGetter(int i) {
		return getters[i];
	}
}
