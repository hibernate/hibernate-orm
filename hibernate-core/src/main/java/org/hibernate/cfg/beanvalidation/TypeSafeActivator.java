/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cfg.beanvalidation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.PropertyDescriptor;

import org.jboss.logging.Logger;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cfg.beanvalidation.ddl.DigitsSchemaConstraint;
import org.hibernate.cfg.beanvalidation.ddl.LengthSchemaConstraint;
import org.hibernate.cfg.beanvalidation.ddl.MaxSchemaConstraint;
import org.hibernate.cfg.beanvalidation.ddl.MinSchemaConstraint;
import org.hibernate.cfg.beanvalidation.ddl.NotNullSchemaConstraint;
import org.hibernate.cfg.beanvalidation.ddl.SchemaConstraint;
import org.hibernate.cfg.beanvalidation.ddl.SizeSchemaConstraint;
import org.hibernate.dialect.Dialect;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.classloading.spi.ClassLoadingException;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
class TypeSafeActivator {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			TypeSafeActivator.class.getName()
	);

	private static final String FACTORY_PROPERTY = "javax.persistence.validation.factory";
	private static final List<SchemaConstraint> schemaConstraints;
	private static final NotNullSchemaConstraint notNullSchemaConstraint = new NotNullSchemaConstraint();

	static {
		schemaConstraints = new ArrayList<SchemaConstraint>();
		schemaConstraints.add( new DigitsSchemaConstraint() );
		schemaConstraints.add( new SizeSchemaConstraint() );
		schemaConstraints.add( new MinSchemaConstraint() );
		schemaConstraints.add( new MaxSchemaConstraint() );
		schemaConstraints.add( new LengthSchemaConstraint() );
	}

	/**
	 * Verifies that the specified object is an instance of {@code ValidatorFactory}.
	 * <p>
	 * Note:</br>
	 * The check is done here to avoid a hard link to Bean Validation
	 * </p>
	 *
	 * @param object the object to check
	 *
	 * @see BeanValidationIntegrator#validateFactory(Object)
	 */
	@SuppressWarnings({ "UnusedDeclaration" })
	public static void assertObjectIsValidatorFactoryInstance(Object object) {
		if ( object == null ) {
			throw new IllegalArgumentException( "null cannot be a valid ValidatorFactory" );
		}

		if ( !ValidatorFactory.class.isInstance( object ) ) {
			throw new HibernateException(
					"Given object was not an instance of " + ValidatorFactory.class.getName() + "[" + object.getClass()
							.getName() + "]"
			);
		}
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	public static void activateBeanValidation(EventListenerRegistry listenerRegistry, Properties properties) {
		ValidatorFactory factory = getValidatorFactory( properties );
		BeanValidationEventListener listener = new BeanValidationEventListener(
				factory, properties
		);

		listenerRegistry.addDuplicationStrategy( DuplicationStrategyImpl.INSTANCE );

		listenerRegistry.appendListeners( EventType.PRE_INSERT, listener );
		listenerRegistry.appendListeners( EventType.PRE_UPDATE, listener );
		listenerRegistry.appendListeners( EventType.PRE_DELETE, listener );

		listener.initialize( properties );
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	// see BeanValidationIntegrator#applyRelationalConstraints
	public static void applyDDL(Collection<PersistentClass> persistentClasses, Properties properties, Dialect dialect) {
		ValidatorFactory factory = getValidatorFactory( properties );
		Class<?>[] groupsArray = new GroupsPerOperation( properties ).get( GroupsPerOperation.Operation.DDL );
		Set<Class<?>> groups = new HashSet<Class<?>>( Arrays.asList( groupsArray ) );

		for ( PersistentClass persistentClass : persistentClasses ) {
			final String className = persistentClass.getClassName();

			if ( className == null || className.length() == 0 ) {
				continue;
			}
			Class<?> clazz;
			try {
				clazz = ReflectHelper.classForName( className, TypeSafeActivator.class );
			}
			catch ( ClassNotFoundException e ) {
				throw new AssertionFailure( "Entity class not found", e );
			}

			try {
				applyDDL( "", persistentClass, clazz, factory, groups, true, dialect );
			}
			catch ( Exception e ) {
				LOG.unableToApplyConstraints( className, e );
			}
		}
	}

	private static void applyDDL(String prefix,
								 PersistentClass persistentClass,
								 Class<?> clazz,
								 ValidatorFactory factory,
								 Set<Class<?>> groups,
								 boolean activateNotNull,
								 Dialect dialect) {
		final BeanDescriptor descriptor = factory.getValidator().getConstraintsForClass( clazz );
		for ( PropertyDescriptor propertyDesc : descriptor.getConstrainedProperties() ) {
			Property property = findPropertyByName( persistentClass, prefix + propertyDesc.getPropertyName() );
			boolean hasNotNull;
			if ( property != null ) {
				hasNotNull = applyConstraints(
						propertyDesc.getConstraintDescriptors(),
						property,
						propertyDesc,
						groups,
						activateNotNull,
						dialect
				);
				if ( property.isComposite() && propertyDesc.isCascaded() ) {
					Class<?> componentClass = ( ( Component ) property.getValue() ).getComponentClass();

					/*
					 * we can apply not null if the upper component let's us activate not null
					 * and if the property is not null.
					 * Otherwise, all sub columns should be left nullable
					 */
					final boolean canSetNotNullOnColumns = activateNotNull && hasNotNull;
					applyDDL(
							prefix + propertyDesc.getPropertyName() + ".",
							persistentClass,
							componentClass,
							factory,
							groups,
							canSetNotNullOnColumns,
							dialect
					);
				}
				//FIXME add collection of components
			}
		}
	}

	private static boolean applyConstraints(Set<ConstraintDescriptor<?>> constraintDescriptors,
											Property property,
											PropertyDescriptor propertyDescriptor,
											Set<Class<?>> groups,
											boolean canApplyNotNull,
											Dialect dialect) {
		boolean hasNotNull = false;
		for ( ConstraintDescriptor<?> constraintDescriptor : constraintDescriptors ) {
			if ( groups != null && Collections.disjoint( constraintDescriptor.getGroups(), groups ) ) {
				continue;
			}

			if ( canApplyNotNull ) {
				hasNotNull = hasNotNull || notNullSchemaConstraint.applyConstraint(
						property,
						constraintDescriptor,
						propertyDescriptor,
						dialect
				);
			}

			for ( SchemaConstraint schemaConstraint : schemaConstraints ) {
				schemaConstraint.applyConstraint( property, constraintDescriptor, propertyDescriptor, dialect );
			}

			// pass an empty set as composing constraints inherit the main constraint and thus are matching already
			hasNotNull = hasNotNull || applyConstraints(
					constraintDescriptor.getComposingConstraints(),
					property, propertyDescriptor, null,
					canApplyNotNull,
					dialect
			);
		}
		return hasNotNull;
	}


	@SuppressWarnings({ "UnusedDeclaration" })
	// see BeanValidationIntegrator#applyRelationalConstraints
	public static void applyDDL(Iterable<EntityBinding> bindings,
								Properties properties,
								ClassLoaderService classLoaderService,
								Dialect dialect) {
		final ValidatorFactory factory = getValidatorFactory( properties );
		final Class<?>[] groupsArray = new GroupsPerOperation( properties ).get( GroupsPerOperation.Operation.DDL );
		final Set<Class<?>> groups = new HashSet<Class<?>>( Arrays.asList( groupsArray ) );

		for ( EntityBinding binding : bindings ) {
			final String className = binding.getEntity().getClassName();
			Class<?> clazz;
			try {
				clazz = classLoaderService.classForName( className );
			}
			catch ( ClassLoadingException error ) {
				throw new AssertionFailure( "Entity class not found", error );
			}
			try {
				applyDDL( binding, clazz, factory, groups, dialect );
			}
			catch ( Exception error ) {
				LOG.unableToApplyConstraints( className, error );
			}
		}
	}

	private static void applyDDL(EntityBinding entityBinding,
								 Class<?> clazz,
								 ValidatorFactory factory,
								 Set<Class<?>> groups,
								 Dialect dialect) {
		final BeanDescriptor descriptor = factory.getValidator().getConstraintsForClass( clazz );
		for ( PropertyDescriptor propertyDescriptor : descriptor.getConstrainedProperties() ) {
			AttributeBinding attributeBinding = entityBinding.locateAttributeBinding( propertyDescriptor.getPropertyName() );
			if ( attributeBinding != null ) {
				applyConstraints( propertyDescriptor, groups, attributeBinding, dialect );
			}
		}
	}

	private static void applyConstraints(PropertyDescriptor propertyDescriptor,
										 Set<Class<?>> groups,
										 AttributeBinding attributeBinding,
										 Dialect dialect) {

		for ( ConstraintDescriptor<?> constraintDescriptor : propertyDescriptor.getConstraintDescriptors() ) {
			if ( groups != null && Collections.disjoint( constraintDescriptor.getGroups(), groups ) ) {
				continue;
			}

			for ( SchemaConstraint schemaConstraint : schemaConstraints ) {
				schemaConstraint.applyConstraint( attributeBinding, constraintDescriptor, propertyDescriptor, dialect );
			}
		}
	}

	/**
	 * Locates a mapping property of a persistent class by property name
	 *
	 * @param associatedClass the persistent class
	 * @param propertyName the property name
	 *
	 * @return the property by path in a recursive way, including IdentifierProperty in the loop if propertyName is
	 *         <code>null</code>.  If propertyName is <code>null</code> or empty, the IdentifierProperty is returned
	 */
	private static Property findPropertyByName(PersistentClass associatedClass, String propertyName) {
		Property property = null;
		Property idProperty = associatedClass.getIdentifierProperty();
		String idName = idProperty != null ? idProperty.getName() : null;
		try {
			if ( propertyName == null
					|| propertyName.length() == 0
					|| propertyName.equals( idName ) ) {
				//default to id
				property = idProperty;
			}
			else {
				if ( propertyName.indexOf( idName + "." ) == 0 ) {
					property = idProperty;
					propertyName = propertyName.substring( idName.length() + 1 );
				}
				StringTokenizer st = new StringTokenizer( propertyName, ".", false );
				while ( st.hasMoreElements() ) {
					String element = ( String ) st.nextElement();
					if ( property == null ) {
						property = associatedClass.getProperty( element );
					}
					else {
						if ( !property.isComposite() ) {
							return null;
						}
						property = ( ( Component ) property.getValue() ).getProperty( element );
					}
				}
			}
		}
		catch ( MappingException e ) {
			try {
				//if we do not find it try to check the identifier mapper
				if ( associatedClass.getIdentifierMapper() == null ) {
					return null;
				}
				StringTokenizer st = new StringTokenizer( propertyName, ".", false );
				while ( st.hasMoreElements() ) {
					String element = ( String ) st.nextElement();
					if ( property == null ) {
						property = associatedClass.getIdentifierMapper().getProperty( element );
					}
					else {
						if ( !property.isComposite() ) {
							return null;
						}
						property = ( ( Component ) property.getValue() ).getProperty( element );
					}
				}
			}
			catch ( MappingException ee ) {
				return null;
			}
		}
		return property;
	}

	// TODO - remove!?

	/**
	 * @param entityBinding entity binding for the currently processed entity
	 * @param attrName
	 *
	 * @return the attribute by path in a recursive way, including EntityIdentifier in the loop if attrName is
	 *         {@code null}.  If attrName is {@code null} or empty, the EntityIdentifier is returned
	 */
	private static AttributeBinding findAttributeBindingByName(EntityBinding entityBinding,
															   String attrName) {
		AttributeBinding attrBinding = null;
		EntityIdentifier identifier = entityBinding.getHierarchyDetails().getEntityIdentifier();
		BasicAttributeBinding idAttrBinding = null; //identifier.getValueBinding();
		String idAttrName = idAttrBinding != null ? idAttrBinding.getAttribute().getName() : null;
		try {
			if ( attrName == null || attrName.length() == 0 || attrName.equals( idAttrName ) ) {
				attrBinding = idAttrBinding; // default to id
			}
			else {
				if ( attrName.indexOf( idAttrName + "." ) == 0 ) {
					attrBinding = idAttrBinding;
					attrName = attrName.substring( idAttrName.length() + 1 );
				}
				for ( StringTokenizer st = new StringTokenizer( attrName, "." ); st.hasMoreElements(); ) {
					String element = st.nextToken();
					if ( attrBinding == null ) {
						attrBinding = entityBinding.locateAttributeBinding( element );
					}
					else {
						return null; // TODO: if (attrBinding.isComposite()) ...
					}
				}
			}
		}
		catch ( MappingException error ) {
			try {
				//if we do not find it try to check the identifier mapper
				if ( !identifier.isIdentifierMapper() ) {
					return null;
				}
				// TODO: finish once composite/embedded/component IDs get worked out
			}
			catch ( MappingException ee ) {
				return null;
			}
		}
		return attrBinding;
	}

	private static ValidatorFactory getValidatorFactory(Map<Object, Object> properties) {
		ValidatorFactory factory = null;
		if ( properties != null ) {
			Object unsafeProperty = properties.get( FACTORY_PROPERTY );
			if ( unsafeProperty != null ) {
				try {
					factory = ValidatorFactory.class.cast( unsafeProperty );
				}
				catch ( ClassCastException e ) {
					throw new HibernateException(
							"Property " + FACTORY_PROPERTY
									+ " should contain an object of type " + ValidatorFactory.class.getName()
					);
				}
			}
		}
		if ( factory == null ) {
			try {
				factory = Validation.buildDefaultValidatorFactory();
			}
			catch ( Exception e ) {
				throw new HibernateException( "Unable to build the default ValidatorFactory", e );
			}
		}
		return factory;
	}
}
