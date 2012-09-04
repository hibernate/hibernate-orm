/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.metamodel.internal.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.jboss.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.metamodel.internal.AbstractIdentifiableType;
import org.hibernate.jpa.metamodel.internal.EmbeddableTypeImpl;
import org.hibernate.jpa.metamodel.internal.EntityTypeImpl;
import org.hibernate.jpa.metamodel.internal.JpaMetaModelPopulationSetting;
import org.hibernate.jpa.metamodel.internal.MappedSuperclassTypeImpl;
import org.hibernate.jpa.metamodel.internal.MetamodelImpl;
import org.hibernate.jpa.metamodel.internal.UnsupportedFeature;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.HierarchyDetails;
import org.hibernate.metamodel.spi.binding.NonAggregatedCompositeAttributeBinding;
import org.hibernate.metamodel.spi.binding.SingularNonAssociationAttributeBinding;
import org.hibernate.metamodel.spi.domain.Entity;
import org.hibernate.metamodel.spi.domain.Hierarchical;
import org.hibernate.metamodel.spi.domain.Superclass;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Defines the contract for building JPA Metamodel ({@link org.hibernate.jpa.metamodel.internal.MetamodelImpl}).
 * <p/>
 * Contract is made up of:<ol>
 *     <li>Instantiating this builder</li>
 *     <li>adding all managed mapping classes to it ({@link #add})</li>
 *     <li>calling {@link #buildMetamodel}</li>
 * </ol>
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class MetamodelBuilder {
	private static final Logger log = Logger.getLogger( MetamodelBuilder.class );

	private final SessionFactoryImplementor sessionFactory;

	// these maps eventually make up the JPA Metamodel
	private final Map<Class<?>,EntityTypeImpl<?>> entityTypeMap = new HashMap<Class<?>, EntityTypeImpl<?>>();
	private final Map<Class<?>, EmbeddableTypeImpl<?>> embeddableTypeMap = new HashMap<Class<?>, EmbeddableTypeImpl<?>>();
	private final Map<Class<?>, MappedSuperclassTypeImpl<?>> mappedSuperclassTypeMap = new HashMap<Class<?>, MappedSuperclassTypeImpl<?>>();

	// these fields are needed just for the duration of building the metamodel
	private final JpaMetaModelPopulationSetting populationSetting;
	private final AttributeBuilder attributeBuilder;
	private final Map<String,EntityTypeImpl> entityTypeByNameMap = new HashMap<String, EntityTypeImpl>();
	private final Map<MappedSuperclassTypeImpl,String> mappedSuperclassEntityNameMap = new HashMap<MappedSuperclassTypeImpl, String>();
	private List<EntityBinding> entityBindingList = new ArrayList<EntityBinding>();
	private Set<Hierarchical> alreadyProcessed = new HashSet<Hierarchical>();


	public MetamodelBuilder(SessionFactoryImplementor sessionFactory, JpaMetaModelPopulationSetting populationSetting) {
		this.sessionFactory = sessionFactory;
		this.populationSetting = populationSetting;
		this.attributeBuilder = new AttributeBuilder( new AttributeBuilderContext() );
	}

	public void add(EntityBinding entityBinding) {
 		locateOrBuildEntityType( entityBinding );
		entityBindingList.add( entityBinding );
	}

	private EntityTypeImpl locateOrBuildEntityType(EntityBinding binding) {
		EntityTypeImpl entityType = entityTypeMap.get( binding.getClassReference() );
		if ( entityType == null ) {
			entityType = buildEntityType( binding );
		}
		return entityType;
	}

	@SuppressWarnings("unchecked")
	private EntityTypeImpl buildEntityType(EntityBinding entityBinding) {
		final Class javaType = entityBinding.getClassReference();
		final AbstractIdentifiableType superType = locateOrBuildSuperType( entityBinding.getEntity().getSuperType(), entityBinding );

		EntityTypeImpl entityType = new EntityTypeImpl(
				javaType,
				superType,
				entityBinding.getJpaEntityName(),
				entityBinding.getHierarchyDetails().getEntityIdentifier().getAttributeBinding() != null,
				entityBinding.isVersioned()
		);

		entityTypeMap.put( javaType, entityType );
		return entityType;
	}

	/**
	 * IMPORTANT!!! This method is called in 2 somewhat different cases:<ol>
	 *     <li>resolving super class for @Entity</li>
	 *     <li>resolving super class for @MappedSuperclass</li>
	 * </ol>
	 * Make sure changes fit both uses
	 *
	 * @param superDescriptor Hibernate metamodel descriptor of the super class
	 * @param entityBinding The Hibernate metamodel entity binding; could be describing different class between the
	 * 2 use cases described above.
	 *
	 * @return The super type.
	 */
	private AbstractIdentifiableType locateOrBuildSuperType(Hierarchical superDescriptor, EntityBinding entityBinding) {
		if ( superDescriptor == null ) {
			return null;
		}

		// the super type here could be either a "mapped superclass" or an entity
		if ( Entity.class.isInstance( superDescriptor ) ) {
			// make sure super entity binding points to same...
			final EntityBinding superBinding = entityBinding.getSuperEntityBinding();
			if ( superBinding == null ) {
				throw new IllegalStateException( "EntityBinding with super class of Entity type did not specify super entity binding" );
			}
			if ( superBinding.getEntity() != superDescriptor )  {
				throw new IllegalStateException( "Super entity binding and descriptor referenced different descriptors" );
			}
			return locateOrBuildEntityType( superBinding );
		}
		else if ( Superclass.class.isInstance( superDescriptor ) ) {
			return locateOrBuildMappedSuperclassType( (Superclass) superDescriptor, entityBinding );
		}
		else {
			throw new IllegalStateException(
					"Unexpected type for entity super descriptor; expecting Entity or Superclass, found ["
							+ superDescriptor.getClassName() + "]"
			);
		}
	}

	private MappedSuperclassTypeImpl locateOrBuildMappedSuperclassType(Superclass superDescriptor, EntityBinding entityBinding) {
		MappedSuperclassTypeImpl mappedSuperclassType = mappedSuperclassTypeMap.get( superDescriptor.getClassReference() );
		if ( mappedSuperclassType == null ) {
			mappedSuperclassType = buildMappedSuperclassType( superDescriptor, entityBinding );
		}
		return mappedSuperclassType;
	}

	@SuppressWarnings("unchecked")
	private MappedSuperclassTypeImpl buildMappedSuperclassType(Superclass superDescriptor, EntityBinding entityBinding) {
		final Class javaType = superDescriptor.getClassReference();
		final AbstractIdentifiableType superSuperType = locateOrBuildSuperType( superDescriptor, entityBinding );

		MappedSuperclassTypeImpl mappedSuperclassType = new MappedSuperclassTypeImpl(
				javaType,
				superSuperType,
				entityBinding.getHierarchyDetails().getEntityIdentifier().getAttributeBinding() != null,
				entityBinding.isVersioned()
		);

		mappedSuperclassTypeMap.put( javaType, mappedSuperclassType );
		mappedSuperclassEntityNameMap.put( mappedSuperclassType, entityBinding.getEntity().getName() );
		return mappedSuperclassType;
	}

	public MetamodelImpl buildMetamodel() {
		log.trace( "Building JPA Metamodel instance..." );
		// we need to process types from superclasses to subclasses
		for ( EntityBinding entityBinding : entityBindingList ) {
			processHierarchy( entityBinding );
		}

		for ( EmbeddableTypeImpl embeddable : embeddableTypeMap.values() ) {
			populateStaticMetamodel( embeddable );
		}


		return new MetamodelImpl(
				entityTypeMap,
				mappedSuperclassTypeMap,
				embeddableTypeMap
		);
	}

	private void processHierarchy(EntityBinding entityBinding) {
		log.trace( "  Starting binding [" + entityBinding.getEntity().getName() + "]" );
		processType( entityBinding.getEntity(), entityBinding );
	}

	/**
	 * Performs a depth-first traversal of the super types...
	 *
	 * @param descriptor The type descriptor to process
	 * @param entityBinding
	 */
	private void processType(Hierarchical descriptor, EntityBinding entityBinding) {
		if ( descriptor == null ) {
			return;
		}

		if ( alreadyProcessed.contains( descriptor ) ) {
			return;
		}
		alreadyProcessed.add( descriptor );

		// perform a depth-first traversal of the super types...
		processSuperType( descriptor, entityBinding );

		final AbstractIdentifiableType jpaDescriptor = Entity.class.isInstance( descriptor )
				? entityTypeMap.get( descriptor.getClassReference() )
				: mappedSuperclassTypeMap.get(  descriptor.getClassReference() );

		applyIdMetadata( descriptor, entityBinding.getHierarchyDetails(), jpaDescriptor );
		applyVersionAttribute( descriptor, entityBinding.getHierarchyDetails(), jpaDescriptor );

		for ( AttributeBinding attributeBinding : entityBinding.attributeBindings() ) {
			if ( entityBinding.getHierarchyDetails().getEntityIdentifier().isIdentifierAttributeBinding( attributeBinding ) ) {
				continue;
			}
			if ( attributeBinding == entityBinding.getHierarchyDetails().getEntityVersion().getVersioningAttributeBinding() ) {
				// skip the version property, it was already handled previously.
				continue;
			}
			final Attribute attribute = attributeBuilder.buildAttribute( jpaDescriptor, attributeBinding );
			if ( attribute != null ) {
				//noinspection unchecked
				jpaDescriptor.getBuilder().addAttribute( attribute );
			}
		}
		jpaDescriptor.lock();
		populateStaticMetamodel( jpaDescriptor );
	}

	private void processSuperType(Hierarchical descriptor, EntityBinding entityBinding) {
		final Hierarchical superDescriptor = descriptor.getSuperType();
		final EntityBinding superEntityBinding = Entity.class.isInstance( superDescriptor )
				? entityBinding.getSuperEntityBinding()
				: entityBinding;
		processType( superDescriptor, superEntityBinding );
	}

	private void applyIdMetadata(
			Hierarchical descriptor,
			HierarchyDetails hierarchyDetails,
			AbstractIdentifiableType jpaDescriptor) {
		switch ( hierarchyDetails.getEntityIdentifier().getNature() ) {
			case SIMPLE: {
				SingularNonAssociationAttributeBinding idAttributeBinding = hierarchyDetails.getEntityIdentifier().getAttributeBinding();
				if ( idAttributeBinding != null ) {
					if ( idAttributeBinding.getAttribute().getAttributeContainer().equals( descriptor ) ) {
						//noinspection unchecked
						jpaDescriptor.getBuilder().applyIdAttribute(
								attributeBuilder.buildIdAttribute( jpaDescriptor, idAttributeBinding )
						);
					}
				}
				break;
			}
			case AGGREGATED_COMPOSITE: {
				SingularNonAssociationAttributeBinding idAttributeBinding = hierarchyDetails.getEntityIdentifier().getAttributeBinding();
				if ( idAttributeBinding != null ) {
					if ( idAttributeBinding.getAttribute().getAttributeContainer().equals( descriptor ) ) {
						//noinspection unchecked
						jpaDescriptor.getBuilder().applyIdAttribute(
								attributeBuilder.buildIdAttribute( jpaDescriptor, idAttributeBinding )
						);
					}
				}
				break;
			}
			default: {
				// nature == (non-aggregated) COMPOSITE
				NonAggregatedCompositeAttributeBinding idAttributeBinding = (NonAggregatedCompositeAttributeBinding) hierarchyDetails.getEntityIdentifier().getAttributeBinding();
				if ( idAttributeBinding != null ) {
					if ( idAttributeBinding.getAttribute().getAttributeContainer().equals( descriptor ) ) {
						Set<SingularAttribute> idClassAttributes = new HashSet<SingularAttribute>();
						for ( AttributeBinding idClassAttributeBinding : idAttributeBinding.attributeBindings() ) {
							idClassAttributes.add( attributeBuilder.buildIdAttribute( jpaDescriptor, idClassAttributeBinding ) );
						}
						//noinspection unchecked
						jpaDescriptor.getBuilder().applyIdClassAttributes( idClassAttributes );
					}
				}
			}
		}
	}

	private void applyVersionAttribute(
			Hierarchical descriptor,
			HierarchyDetails hierarchyDetails,
			AbstractIdentifiableType jpaDescriptor) {
		final BasicAttributeBinding versionBinding = hierarchyDetails.getEntityVersion().getVersioningAttributeBinding();
		if ( versionBinding != null ) {
			if ( versionBinding.getAttribute().getAttributeContainer().equals( descriptor ) ) {
				//noinspection unchecked
				jpaDescriptor.getBuilder().applyVersionAttribute(
						attributeBuilder.buildVersionAttribute( jpaDescriptor, versionBinding )
				);
			}
		}
	}

	private void populateStaticMetamodel(AbstractIdentifiableType jpaDescriptor) {
		if ( populationSetting == JpaMetaModelPopulationSetting.DISABLED ) {
			return;
		}

		// todo : implement !
	}

	private void populateStaticMetamodel(EmbeddableTypeImpl embeddable) {
		if ( populationSetting == JpaMetaModelPopulationSetting.DISABLED ) {
			return;
		}

		// todo : implement !
	}


	/**
	 * Implementation of AttributeBuilder.Context
	 */
	class AttributeBuilderContext implements AttributeBuilder.Context {
		public AttributeBuilderContext() {
		}

		public Type locateEntityTypeByName(String entityName) {
			return entityTypeByNameMap.get( entityName );
		}

		@Override
		public void registerEmbeddedableType(EmbeddableTypeImpl embeddableType) {
			embeddableTypeMap.put( embeddableType.getJavaType(), embeddableType );
		}

		public SessionFactoryImplementor getSessionFactory() {
			return sessionFactory;
		}

		@Override
		public EntityPersister getSubClassEntityPersister(MappedSuperclassTypeImpl mappedSuperclass) {
			final String entityName = mappedSuperclassEntityNameMap.get( mappedSuperclass );
			if ( entityName == null ) {
				throw new HibernateException(
						String.format(
								"Could not resolve @MappedSuperclass [%s] to entity name",
								mappedSuperclass.getJavaType().getName()
						)
				);
			}
			final EntityPersister entityPersister = sessionFactory.getEntityPersister( entityName );
			if ( entityPersister == null ) {
				throw new HibernateException(
						String.format(
								"Unable to resolve entity name [%s] to EntityPersister for @MappedSuperclass [%s]",
								entityName,
								mappedSuperclass.getJavaType().getName()
						)
				);
			}
			return entityPersister;
		}

		@Override
		public void handleUnsupportedFeature(UnsupportedFeature feature) {
			if ( populationSetting == JpaMetaModelPopulationSetting.IGNORE_UNSUPPORTED ) {
				log.debug( "Ignoring mapping construct not supported as part of JPA metamodel [" + feature.getMessage() + "]" );
			}
			else {
				throw new UnsupportedOperationException( feature.getMessage() );
			}
		}
	}

}
