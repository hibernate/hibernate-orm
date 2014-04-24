/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.internal.metamodel.builder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.annotations.common.AssertionFailure;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.internal.metamodel.AbstractIdentifiableType;
import org.hibernate.jpa.internal.metamodel.AbstractManagedType;
import org.hibernate.jpa.internal.metamodel.EmbeddableTypeImpl;
import org.hibernate.jpa.internal.metamodel.EntityTypeImpl;
import org.hibernate.jpa.internal.metamodel.MappedSuperclassTypeImpl;
import org.hibernate.jpa.internal.metamodel.MetamodelImpl;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.EmbeddableBinding;
import org.hibernate.metamodel.spi.binding.EmbeddedAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;
import org.hibernate.metamodel.spi.binding.HierarchyDetails;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.domain.Entity;
import org.hibernate.metamodel.spi.domain.Hierarchical;
import org.hibernate.metamodel.spi.domain.MappedSuperclass;
import org.hibernate.persister.entity.EntityPersister;

import org.jboss.logging.Logger;

/**
 * Defines the contract for building JPA Metamodel
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
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			MetamodelBuilder.class.getName()
	);
	private final SessionFactoryImplementor sessionFactory;
	private final ClassLoaderService classLoaderService;


	// these maps eventually make up the JPA Metamodel
	private final Map<Class<?>,EntityTypeImpl<?>> entityTypeMap = new HashMap<Class<?>, EntityTypeImpl<?>>();
	private final Map<Class<?>, EmbeddableTypeImpl<?>> embeddableTypeMap = new HashMap<Class<?>, EmbeddableTypeImpl<?>>();
	private final Map<Class<?>, MappedSuperclassTypeImpl<?>> mappedSuperclassTypeMap = new HashMap<Class<?>, MappedSuperclassTypeImpl<?>>();

	// these fields are needed just for the duration of building the metamodel
	private final JpaMetaModelPopulationSetting populationSetting;
	private final AttributeBuilder attributeBuilder;
	private final Map<String,EntityTypeImpl<?>> entityTypeByNameMap = new HashMap<String, EntityTypeImpl<?>>();
	private final Map<MappedSuperclassTypeImpl,String> mappedSuperclassEntityNameMap = new HashMap<MappedSuperclassTypeImpl, String>();
	private List<EntityBinding> entityBindingList = new ArrayList<EntityBinding>();
	private Set<Hierarchical> alreadyProcessed = new HashSet<Hierarchical>();

	public static MetamodelImpl oneShot(
			SessionFactoryImplementor sessionFactory,
			Metadata metadata,
			JpaMetaModelPopulationSetting populationSetting) {
		MetamodelBuilder builder = new MetamodelBuilder( sessionFactory, populationSetting );
		for ( EntityBinding eb : metadata.getEntityBindings() ) {
			builder.add( eb );
		}
		return builder.buildMetamodel();
	}

	public MetamodelBuilder(SessionFactoryImplementor sessionFactory, JpaMetaModelPopulationSetting populationSetting) {
		this.sessionFactory = sessionFactory;
		this.classLoaderService = sessionFactory.getServiceRegistry().getService( ClassLoaderService.class );
		this.populationSetting = populationSetting;
		this.attributeBuilder = new AttributeBuilder( new AttributeBuilderContext() );
	}

	public void add(EntityBinding entityBinding) {
		if ( entityBinding.getHierarchyDetails().getEntityMode() == EntityMode.POJO ) {
			locateOrBuildEntityType( entityBinding );
		}
		entityBindingList.add( entityBinding );
	}

	private EntityTypeImpl locateOrBuildEntityType(EntityBinding binding) {
		EntityTypeImpl entityType = entityTypeByNameMap.get( binding.getEntityName() );
		if ( entityType == null ) {
			entityType = buildEntityType( binding );
		}
		return entityType;
	}

	@SuppressWarnings("unchecked")
	private EntityTypeImpl buildEntityType(EntityBinding entityBinding) {
		final ClassLoaderService cls = sessionFactory.getServiceRegistry().getService( ClassLoaderService.class );
		final Class javaType = cls.classForName( entityBinding.getEntity().getDescriptor().getName().toString() );
		final AbstractIdentifiableType superType = locateOrBuildSuperType( entityBinding.getEntity().getSuperType(), entityBinding );

		final EntityIdentifier idInfo = entityBinding.getHierarchyDetails().getEntityIdentifier();

		EntityTypeImpl entityType = new EntityTypeImpl(
				javaType,
				superType,
				entityBinding.getEntityName(),
				entityBinding.getJpaEntityName(),
				idInfo.definesIdClass(),
				idInfo.getNature() != EntityIdentifierNature.NON_AGGREGATED_COMPOSITE,
				entityBinding.getHierarchyDetails().isVersioned()
		);

		entityTypeMap.put( javaType, entityType );
		entityTypeByNameMap.put( entityBinding.getEntityName(), entityType );
		return entityType;
	}

	/**
	 * IMPORTANT!!! This method is called in 2 somewhat different cases:<ol>
	 *     <li>resolving super class for @Entity</li>
	 *     <li>resolving super class for @MappedSuperclass</li>
	 * </ol>
	 * Make sure changes fit both uses
	 *
	 * @param hierarchical Hibernate metamodel descriptor of the super class
	 * @param entityBinding The Hibernate metamodel entity binding; could be describing different class between the
	 * 2 use cases described above.
	 *
	 * @return The super type.
	 */
	private AbstractIdentifiableType locateOrBuildSuperType(Hierarchical hierarchical, EntityBinding entityBinding) {
		if ( hierarchical == null ) {
			return null;
		}

		// the super type here could be either a "mapped superclass" or an entity
		if ( Entity.class.isInstance( hierarchical ) ) {
			// make sure super entity binding points to same...
			final EntityBinding superBinding = entityBinding.getSuperEntityBinding();
			if ( superBinding == null ) {
				throw new IllegalStateException( "EntityBinding with super class of Entity type did not specify super entity binding" );
			}
			if ( superBinding.getEntity() != hierarchical )  {
				throw new IllegalStateException( "Super entity binding and descriptor referenced different descriptors" );
			}
			return locateOrBuildEntityType( superBinding );
		}
		else if ( MappedSuperclass.class.isInstance( hierarchical ) ) {
			return locateOrBuildMappedSuperclassType( (MappedSuperclass) hierarchical, entityBinding );
		}
		else {
			throw new IllegalStateException(
					"Unexpected type for entity super descriptor; expecting Entity or Superclass, found ["
							+ hierarchical.getDescriptor().getName().toString() + "]"
			);
		}
	}

	private MappedSuperclassTypeImpl locateOrBuildMappedSuperclassType(MappedSuperclass mappedSuperclass, EntityBinding entityBinding) {
		MappedSuperclassTypeImpl mappedSuperclassType = mappedSuperclassTypeMap.get(
				loadClass( mappedSuperclass.getDescriptor() )
		);
		if ( mappedSuperclassType == null ) {
			mappedSuperclassType = buildMappedSuperclassType( mappedSuperclass, entityBinding );
		}
		return mappedSuperclassType;
	}

	private Class<?> loadClass(JavaTypeDescriptor descriptor) {
		return classLoaderService.classForName( descriptor.getName().toString() );
	}

	@SuppressWarnings("unchecked")
	private MappedSuperclassTypeImpl buildMappedSuperclassType(MappedSuperclass mappedSuperclass, EntityBinding entityBinding) {
		final Class javaType = loadClass( mappedSuperclass.getDescriptor() );
		final AbstractIdentifiableType superSuperType = locateOrBuildSuperType(
				mappedSuperclass.getSuperType(),
				entityBinding
		);

		final EntityIdentifier idInfo = entityBinding.getHierarchyDetails().getEntityIdentifier();

		MappedSuperclassTypeImpl mappedSuperclassType = new MappedSuperclassTypeImpl(
				javaType,
				superSuperType,
				idInfo.definesIdClass(),
				idInfo.getNature() != EntityIdentifierNature.NON_AGGREGATED_COMPOSITE,
				entityBinding.getHierarchyDetails().isVersioned()
		);

		mappedSuperclassTypeMap.put( javaType, mappedSuperclassType );
		mappedSuperclassEntityNameMap.put( mappedSuperclassType, entityBinding.getEntityName() );
		return mappedSuperclassType;
	}

	public MetamodelImpl buildMetamodel() {
		LOG.trace( "Building JPA Metamodel instance..." );
		// we need to process types from superclasses to subclasses
		for ( EntityBinding entityBinding : entityBindingList ) {
			processHierarchy( entityBinding );
		}

		handleUnusedMappedSuperclasses();

		for ( EmbeddableTypeImpl embeddable : embeddableTypeMap.values() ) {
			populateStaticMetamodel( embeddable );
		}

		return new MetamodelImpl(
				entityTypeMap,
				mappedSuperclassTypeMap,
				embeddableTypeMap,
				entityTypeByNameMap
		);
	}

	private void handleUnusedMappedSuperclasses() {
		// todo : keep running track of MappedSuperclasses as we process them and:
		//		1) warn user about them
		//		2) build a mapping for them
//		final Set<MappedSuperclass> unusedMappedSuperclasses = context.getUnusedMappedSuperclasses();
//		if ( !unusedMappedSuperclasses.isEmpty() ) {
//			for ( MappedSuperclass mappedSuperclass : unusedMappedSuperclasses ) {
//				log.unusedMappedSuperclass( mappedSuperclass.getMappedClass().getName() );
//				locateOrBuildMappedsuperclassType( mappedSuperclass, context );
//			}
//		}
	}

	private void processHierarchy(EntityBinding entityBinding) {
		LOG.trace( "  Starting binding [" + entityBinding.getEntityName() + "]" );
		processType( entityBinding.getEntity(), entityBinding );
	}

	/**
	 * Performs a depth-first traversal of the super types...
	 *
	 * @param hierarchical The type descriptor to process
	 * @param entityBinding
	 */
	private void processType(Hierarchical hierarchical, EntityBinding entityBinding) {
		if ( hierarchical == null ) {
			return;
		}

		if ( alreadyProcessed.contains( hierarchical ) ) {
			return;
		}
		alreadyProcessed.add( hierarchical );

		// perform a depth-first traversal of the super types...
		processSuperType( hierarchical, entityBinding );

		final AbstractIdentifiableType jpaDescriptor = Entity.class.isInstance( hierarchical )
				? entityTypeByNameMap.get( hierarchical.getName() )
				: mappedSuperclassTypeMap.get( loadClass( hierarchical.getDescriptor() ) );
		if ( jpaDescriptor == null && entityBinding.getHierarchyDetails().getEntityMode() != EntityMode.POJO ) {
			return;
		}
		applyIdMetadata( hierarchical, entityBinding.getHierarchyDetails(), jpaDescriptor );
		applyVersionAttribute( hierarchical, entityBinding.getHierarchyDetails(), jpaDescriptor );

		for ( AttributeBinding attributeBinding : entityBinding.attributeBindings() ) {
			if ( entityBinding.getHierarchyDetails().getEntityIdentifier().getEntityIdentifierBinding().isIdentifierAttributeBinding(
					attributeBinding
			) ) {
				continue;
			}
			if ( entityBinding.getHierarchyDetails().isVersioned() ) {
				if ( attributeBinding == entityBinding.getHierarchyDetails().getEntityVersion().getVersioningAttributeBinding() ) {
					// skip the version property, it was already handled previously.
					continue;
				}
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
		final EntityIdentifier idInfo = hierarchyDetails.getEntityIdentifier();
		final EntityIdentifier.Binding idBinding = idInfo.getEntityIdentifierBinding();

		switch ( idBinding.getNature() ) {
			case SIMPLE: {
				final EntityIdentifier.SimpleIdentifierBinding simpleIdBinding =
						(EntityIdentifier.SimpleIdentifierBinding) idBinding;
				final SingularAttributeBinding idAttributeBinding =
						simpleIdBinding.getAttributeBinding();
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
				final EntityIdentifier.AggregatedCompositeIdentifierBinding cIdBinding =
						(EntityIdentifier.AggregatedCompositeIdentifierBinding) idBinding;
				final EmbeddedAttributeBinding idAttributeBinding =
						(EmbeddedAttributeBinding) cIdBinding.getAttributeBinding();

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
				final EntityIdentifier.NonAggregatedCompositeIdentifierBinding cIdBinding =
						(EntityIdentifier.NonAggregatedCompositeIdentifierBinding) idBinding;
				final EmbeddableBinding virtualIdEmbeddableBinding =
						cIdBinding.getVirtualEmbeddableBinding();

				Set<SingularAttribute> idClassAttributes = new HashSet<SingularAttribute>();
				for ( AttributeBinding attributeBinding : virtualIdEmbeddableBinding.attributeBindings() ) {
					idClassAttributes.add( attributeBuilder.buildIdAttribute( jpaDescriptor, attributeBinding ) );
				}
				//noinspection unchecked
				jpaDescriptor.getBuilder().applyIdClassAttributes( idClassAttributes );
			}
		}
	}

	private void applyVersionAttribute(
			Hierarchical descriptor,
			HierarchyDetails hierarchyDetails,
			AbstractIdentifiableType jpaDescriptor) {
		if ( !hierarchyDetails.isVersioned() ) {
			return;
		}

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

	private void populateStaticMetamodel(AbstractManagedType jpaDescriptor) {
		if ( populationSetting == JpaMetaModelPopulationSetting.DISABLED ) {
			return;
		}
		final Class managedTypeClass = jpaDescriptor.getJavaType();
		final String metamodelClassName = managedTypeClass.getName() + "_";
		try {
			final Class metamodelClass = Class.forName( metamodelClassName, true, managedTypeClass.getClassLoader() );
			// we found the class; so populate it...
			registerAttributes( metamodelClass, jpaDescriptor );
		}
		catch ( ClassNotFoundException ignore ) {
			// nothing to do...
		}

		AbstractManagedType superType = jpaDescriptor.getSupertype();
		if ( superType != null ) {
			populateStaticMetamodel( superType );
		}

	}


	private final Set<Class> processedMetamodelClasses = new HashSet<Class>();

	private <X> void registerAttributes(Class metamodelClass, AbstractManagedType managedType) {
		if ( !processedMetamodelClasses.add( metamodelClass ) ) {
			return;
		}

		// push the attributes on to the metamodel class...
		for ( Object attribute : managedType.getDeclaredAttributes() ) {

			registerAttribute( metamodelClass, (Attribute<X, ?>) attribute );
		}
		if ( IdentifiableType.class.isInstance( managedType ) ) {
			final AbstractIdentifiableType<X> entityType = (AbstractIdentifiableType<X>) managedType;
			// handle version
			if ( entityType.hasDeclaredVersionAttribute() ) {
				registerAttribute( metamodelClass, entityType.getDeclaredVersion() );
			}

			// handle id-class mappings specially
			if ( !entityType.hasSingleIdAttribute() ) {
				final Set<SingularAttribute<? super X, ?>> attributes = entityType.getIdClassAttributes();
				if ( attributes != null ) {
					for ( SingularAttribute<? super X, ?> attribute : attributes ) {
						registerAttribute( metamodelClass, attribute );
					}
				}
			}
		}


	}

	private <X> void registerAttribute(Class metamodelClass, Attribute<X, ?> attribute) {
		final String name = attribute.getName();
		try {
			// there is a shortcoming in the existing Hibernate code in terms of the way MappedSuperclass
			// support was bolted on which comes to bear right here when the attribute is an embeddable type
			// defined on a MappedSuperclass.  We do not have the correct information to determine the
			// appropriate attribute declarer in such cases and so the incoming metamodelClass most likely
			// does not represent the declarer in such cases.
			//
			// As a result, in the case of embeddable classes we simply use getField rather than get
			// getDeclaredField
			final Field field = attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.EMBEDDED
					? metamodelClass.getField( name )
					: metamodelClass.getDeclaredField( name );
			try {
				if ( ! field.isAccessible() ) {
					// should be public anyway, but to be sure...
					field.setAccessible( true );
				}
				field.set( null, attribute );
			}
			catch ( IllegalAccessException e ) {
				// todo : exception type?
				throw new AssertionFailure(
						"Unable to inject static metamodel attribute : " + metamodelClass.getName() + '#' + name,
						e
				);
			}
			catch ( IllegalArgumentException e ) {
				// most likely a mismatch in the type we are injecting and the defined field; this represents a
				// mismatch in how the annotation processor interpretted the attribute and how our metamodel
				// and/or annotation binder did.

//              This is particularly the case as arrays are nto handled propery by the StaticMetamodel generator

//				throw new AssertionFailure(
//						"Illegal argument on static metamodel field injection : " + metamodelClass.getName() + '#' + name
//								+ "; expected type :  " + attribute.getClass().getName()
//								+ "; encountered type : " + field.getType().getName()
//				);
				LOG.illegalArgumentOnStaticMetamodelFieldInjection(metamodelClass.getName(),
						name,
						attribute.getClass().getName(),
						field.getType().getName());
			}
		}
		catch ( NoSuchFieldException e ) {
			LOG.unableToLocateStaticMetamodelField(metamodelClass.getName(), name);
//			throw new AssertionFailure(
//					"Unable to locate static metamodel field : " + metamodelClass.getName() + '#' + name
//			);
		}
	}


//	private void populateStaticMetamodel(EmbeddableTypeImpl embeddable) {
//		if ( populationSetting == JpaMetaModelPopulationSetting.DISABLED ) {
//			return;
//		}
//		final Class managedTypeClass = embeddable.getJavaType();
//		final String metamodelClassName = managedTypeClass.getName() + "_";
//		try {
//			final Class metamodelClass = Class.forName( metamodelClassName, true, managedTypeClass.getClassLoader() );
//			// we found the class; so populate it...
//			registerAttributes( metamodelClass, embeddable );
//		}
//		catch ( ClassNotFoundException ignore ) {
//			// nothing to do...
//		}
//
//		AbstractManagedType superType = embeddable.getSupertype();
//		if ( superType != null ) {
//			populateStaticMetamodel( superType );
//		}
//	}


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
				LOG.debug( "Ignoring mapping construct not supported as part of JPA metamodel [" + feature.getMessage() + "]" );
			}
			else {
				throw new UnsupportedOperationException( feature.getMessage() );
			}
		}
	}

}
