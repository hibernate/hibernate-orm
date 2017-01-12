/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.spi;

import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.type.descriptor.java.internal.JavaTypeDescriptorBaseline;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.TypeConfigurationAware;

import org.jboss.logging.Logger;

/**
 * Basically a map from {@link Class} -> {@link JavaTypeDescriptor}
 *
 * @author Steve Ebersole
 */
public class JavaTypeDescriptorRegistry implements JavaTypeDescriptorBaseline.BaselineTarget {
	private static final Logger log = Logger.getLogger( JavaTypeDescriptorRegistry.class );

	private final TypeConfiguration typeConfiguration;
	private final ConcurrentHashMap<String,JavaTypeDescriptor> descriptorsByName = new ConcurrentHashMap<>();

	public JavaTypeDescriptorRegistry(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
		JavaTypeDescriptorBaseline.prime( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// baseline descriptors

	@Override
	public void addBaselineDescriptor(BasicJavaDescriptor descriptor) {
		if ( descriptor.getJavaType() == null ) {
			throw new IllegalStateException( "Illegal to add BasicJavaTypeDescriptor with null Java type" );
		}
		addBaselineDescriptor( (Class) descriptor.getJavaType(), descriptor );
	}

	@Override
	public void addBaselineDescriptor(Class describedJavaType, BasicJavaDescriptor descriptor) {
		performInjections( descriptor );
		descriptorsByName.put( describedJavaType.getName(), descriptor );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// descriptor access

	@SuppressWarnings("unchecked")
	public <T> JavaTypeDescriptor<T> getDescriptor(Class<T> javaType) {
		if ( javaType == null ) {
			throw new IllegalArgumentException( "Class passed to locate Java type descriptor cannot be null" );
		}
		return descriptorsByName.get( javaType.getName() );
	}

	@SuppressWarnings("unchecked")
	public <T> JavaTypeDescriptor<T> getDescriptor(String javaTypeName) {
		if ( javaTypeName == null ) {
			throw new IllegalArgumentException( "Java type name passed to locate Java type descriptor cannot be null" );
		}
		return descriptorsByName.get( javaTypeName );
	}

//	@SuppressWarnings("unchecked")
//	public <T> JavaTypeDescriptor<T> getDescriptor(String typeName) {
//		return descriptorsByName.get( typeName );
//	}
//
//	@SuppressWarnings("unchecked")
//	public <T> JavaTypeDescriptor<T> getDescriptor(Class<T> cls) {
//		if ( cls == null ) {
//			throw new IllegalArgumentException( "Class passed to locate Java type descriptor cannot be null" );
//		}
//
//		JavaTypeDescriptor<T> descriptor = descriptorsByName.get( cls.getName() );
//		if ( descriptor != null ) {
//			return descriptor;
//		}
//
//		if ( cls.isEnum() ) {
//			descriptor = new EnumJavaTypeDescriptor( cls );
//			descriptorsByName.put( cls.getName(), descriptor );
//			return descriptor;
//		}
//
//		if ( Serializable.class.isAssignableFrom( cls ) ) {
//			return new SerializableTypeDescriptor( cls );
//		}
//
//		// find the first "assignable" match
//		for ( Map.Entry<String,JavaTypeDescriptor> entry : descriptorsByName.entrySet() ) {
//			if ( entry.getValue().getJavaType() == null ) {
//				continue;
//			}
//
//			if ( entry.getValue().getJavaType().isAssignableFrom( cls ) ) {
//				log.debugf( "Using  cached JavaTypeDescriptor instance for Java class [%s]", cls.getName() );
//				return entry.getValue();
//			}
//		}
//
//		log.warnf( "Could not find matching type descriptor for requested Java class [%s]; using fallback", cls.getName() );
//		return new JavaTypeDescriptorBasicAdaptorImpl<>( cls );
//	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// add descriptors

	public void addDescriptor(JavaTypeDescriptor descriptor) {
		addDescriptorInternal( descriptor.getTypeName(), descriptor );
	}

	private void addDescriptorInternal(JavaTypeDescriptor descriptor) {
		addDescriptorInternal( descriptor.getTypeName(), descriptor );
	}

	private void addDescriptorInternal(Class javaType, JavaTypeDescriptor descriptor) {
		addDescriptorInternal( javaType.getName(), descriptor );
	}

	private void addDescriptorInternal(String registrationKey, JavaTypeDescriptor descriptor) {
		performInjections( descriptor );

		final JavaTypeDescriptor old = descriptorsByName.put( registrationKey, descriptor );
		if ( old != null && old!= descriptor ) {
			log.debugf(
					"JavaTypeDescriptorRegistry entry replaced : %s -> %s (was %s)",
					descriptor.getJavaType(),
					descriptor,
					old
			);
		}
	}

	private void performInjections(JavaTypeDescriptor descriptor) {
		if ( descriptor instanceof TypeConfigurationAware ) {
			// would be nice to make the JavaTypeDescriptor for an entity, e.g., aware of the the TypeConfiguration
			( (TypeConfigurationAware) descriptor ).setTypeConfiguration( typeConfiguration );
		}
	}

//	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//	// basic type descriptor factory methods
//
//	@SuppressWarnings("unchecked")
//	public <T> BasicJavaDescriptor<T> makeBasicTypeDescriptor(Class<T> javaType) {
//		return makeBasicTypeDescriptor( javaType, null, null );
//	}
//
//	@SuppressWarnings("unchecked")
//	public <T> BasicJavaDescriptor<T> makeBasicTypeDescriptor(
//			Class<T> javaType,
//			MutabilityPlan<T> mutabilityPlan,
//			Comparator comparator) {
//		if ( javaType == null ) {
//			throw new IllegalArgumentException( "Class passed to locate Java type descriptor cannot be null" );
//		}
//
//		JavaTypeDescriptor<T> typeDescriptor = descriptorsByName.get( javaType.getName() );
//		if ( typeDescriptor != null ) {
//			if ( typeDescriptor.getPersistenceType() != Type.PersistenceType.BASIC ) {
//				throw new HibernateException(
//						"JavaTypeDescriptor was already registered for " + javaType.getName() +
//								" as a non-BasicType (" + typeDescriptor.getPersistenceType().name() + ")"
//				);
//			}
//		}
//		else {
//			typeDescriptor = new JavaTypeDescriptorBasicAdaptorImpl( javaType, mutabilityPlan, comparator );
//			addDescriptorInternal( javaType, typeDescriptor );
//		}
//
//		return (BasicJavaDescriptor<T>) typeDescriptor;
//	}
//
//	private JavaTypeDescriptorEntityImplementor getEntityDescriptor(String typeName) {
//		throw new NotYetImplementedException();
//	}
//
//	public JavaTypeDescriptorEntityImplementor makeRootEntityDescriptor(
//			String typeName,
//			EntityHierarchy.InheritanceStyle inheritanceStyle,
//			EntityMode entityMode) {
//		if ( descriptorsByName.containsKey( typeName ) ) {
//			throw new IllegalStateException( "Root entity descriptor already registered under that name [" + typeName + "]." );
//		}
//		final RootEntityDescriptor descriptor = new RootEntityDescriptor( typeName, inheritanceStyle, entityMode );
//		performInjections( descriptor );
//		descriptorsByName.put( typeName, descriptor );
//		return descriptor;
//	}
//
//	public JavaTypeDescriptorEntityImplementor makeEntityDescriptor(
//			String typeName,
//			JavaTypeDescriptorEntityImplementor javaTypeDescriptor) {
//		if ( descriptorsByName.containsKey( typeName ) ) {
//			throw new IllegalStateException( "Entity descriptor already registered under that name [" + typeName + "]." );
//		}
//		final EntityDescriptor descriptor = new EntityDescriptor( typeName, javaTypeDescriptor.getEntityHierarchy(), null, (ManagedTypeDescriptor) javaTypeDescriptor );
//		performInjections( descriptor );
//		descriptorsByName.put( typeName, descriptor );
//		return descriptor;
//	}
//
//
//	/**
//	 * Legacy code here always worked on the Class for MappedSuperclass; continue that, for now...
//	 *
//	 * @param mappedSuperclassClass The Class reference to the Class annotated with MappedSuperclass
//	 *
//	 * @return The descriptor
//	 */
//	public JavaTypeDescriptorMappedSuperclassImplementor getMappedSuperclassDescriptor(Class mappedSuperclassClass) {
//		JavaTypeDescriptor descriptor = descriptorsByName.get( mappedSuperclassClass.getName() );
//		if ( descriptor == null ) {
//			// todo determine its super-type...
//			final IdentifiableTypeDescriptor superType = resolveSuperManagedTypeDescriptor( mappedSuperclassClass );
//			descriptor = new MappedSuperclassTypeDescriptor( mappedSuperclassClass, superType.getEntityHierarchy(), superType );
//			performInjections( descriptor );
//			descriptorsByName.put( mappedSuperclassClass.getName(), descriptor );
//		}
//		else {
//			if ( !JavaTypeDescriptorMappedSuperclassImplementor.class.isInstance( descriptor ) ) {
//				throw new HibernateException(
//						"Request for JavaTypeDescriptor for class [%s] as a MappedSuperclass " +
//								"encountered a previous registration [%s] that did not indicate MappedSuperclass"
//				);
//			}
//		}
//
//		return (JavaTypeDescriptorMappedSuperclassImplementor) descriptor;
//	}
//
//	private IdentifiableTypeDescriptor resolveSuperManagedTypeDescriptor(Class managedClassType) {
//		Class superType = managedClassType.getSuperclass();
//		while ( superType != null && !Object.class.equals( superType ) ) {
//			final XClass superTypeXClass = typeConfiguration.getTypeConfiguration()
//					.getMetadataBuildingContext()
//					.getBootstrapContext()
//					.getReflectionManager()
//					.toXClass( superType );
//
//			// NOTE - while we eventually want to support composite/embeddable inheritance, we
//			// 		currently do not.  So here we only deal with Entity/MappedSuperclass
//
//			if ( superTypeXClass.getAnnotation( Entity.class ) != null ) {
//				return (EntityDescriptor) getEntityDescriptor( superTypeXClass.getName() );
//			}
//			else if ( superTypeXClass.getAnnotation( MappedSuperclass.class ) != null ) {
//				return (MappedSuperclassTypeDescriptor) getMappedSuperclassDescriptor(
//						typeConfiguration.getTypeConfiguration()
//								.getMetadataBuildingContext()
//								.getBootstrapContext()
//								.getReflectionManager()
//								.toClass( superTypeXClass )
//				);
//			}
//
//			superType = superType.getSuperclass();
//		}
//
//		return null;
//	}
//
//
//	private JavaTypeDescriptorEmbeddableImplementor getEmbeddableDescriptor(String typeName) {
//		throw new NotYetImplementedException( "JavaTypeDescriptor support for @Embeddable not yet implemented" );
//	}
}