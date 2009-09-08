package org.hibernate.cfg.beanvalidation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Collection;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.PropertyDescriptor;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.event.EventListeners;
import org.hibernate.event.PreDeleteEventListener;
import org.hibernate.event.PreInsertEventListener;
import org.hibernate.event.PreUpdateEventListener;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.util.ReflectHelper;

/**
 * @author Emmanuel Bernard
 */
class TypeSafeActivator {

	private static final Logger logger = LoggerFactory.getLogger( TypeSafeActivator.class );

	private static final String FACTORY_PROPERTY = "javax.persistence.validation.factory";

	public static void activateBeanValidation(EventListeners eventListeners, Properties properties) {
		ValidatorFactory factory = getValidatorFactory( properties );
		BeanValidationEventListener beanValidationEventListener = new BeanValidationEventListener( factory, properties );

		{
			PreInsertEventListener[] listeners = eventListeners.getPreInsertEventListeners();
			int length = listeners.length + 1;
			PreInsertEventListener[] newListeners = new PreInsertEventListener[length];
			System.arraycopy( listeners, 0, newListeners, 0, length - 1 );
			newListeners[length - 1] = beanValidationEventListener;
			eventListeners.setPreInsertEventListeners( newListeners );
		}

		{
			PreUpdateEventListener[] listeners = eventListeners.getPreUpdateEventListeners();
			int length = listeners.length + 1;
			PreUpdateEventListener[] newListeners = new PreUpdateEventListener[length];
			System.arraycopy( listeners, 0, newListeners, 0, length - 1 );
			newListeners[length - 1] = beanValidationEventListener;
			eventListeners.setPreUpdateEventListeners( newListeners );
		}

		{
			PreDeleteEventListener[] listeners = eventListeners.getPreDeleteEventListeners();
			int length = listeners.length + 1;
			PreDeleteEventListener[] newListeners = new PreDeleteEventListener[length];
			System.arraycopy( listeners, 0, newListeners, 0, length - 1 );
			newListeners[length - 1] = beanValidationEventListener;
			eventListeners.setPreDeleteEventListeners( newListeners );
		}
	}

	public static void applyDDL(Collection<PersistentClass> persistentClasses, Properties properties) {
		ValidatorFactory factory = getValidatorFactory( properties );
		Class<?>[] groupsArray = new GroupsPerOperation( properties ).get( GroupsPerOperation.Operation.DDL );
		Set<Class<?>> groups = new HashSet<Class<?>>( Arrays.asList( groupsArray ) );

		for ( PersistentClass persistentClass : persistentClasses ) {
			final String className = persistentClass.getClassName();

			if ( className == null || className.length() == 0) continue;
			Class<?> clazz;
			try {
				clazz = ReflectHelper.classForName( className, TypeSafeActivator.class );
			}
			catch ( ClassNotFoundException e ) {
				throw new AssertionFailure( "Entity class not found", e);
			}

			try {
				applyDDL( "", persistentClass, clazz, factory, groups, true );
			}
			catch (Exception e) {
				logger.warn( "Unable to apply constraints on DDL for " + className, e );
			}
		}
	}

	private static void applyDDL(String prefix,
								 PersistentClass persistentClass,
								 Class<?> clazz,
								 ValidatorFactory factory,
								 Set<Class<?>> groups,
								 boolean activateNotNull) {
		final BeanDescriptor descriptor = factory.getValidator().getConstraintsForClass( clazz );
		//no bean level constraints can be applied, go to the properties

		for ( PropertyDescriptor propertyDesc : descriptor.getConstrainedProperties() ) {
			Property property = findPropertyByName( persistentClass, prefix + propertyDesc.getPropertyName() );
			boolean hasNotNull = false;
			if ( property != null ) {
				hasNotNull = applyConstraints( propertyDesc.getConstraintDescriptors(), property, propertyDesc, groups, activateNotNull );
				if ( property.isComposite() && propertyDesc.isCascaded() ) {
					Class<?> componentClass = ( ( Component ) property.getValue() ).getComponentClass();

					/*
					 * we can apply not null if the upper component let's us activate not null
					 * and if the property is not null.
					 * Otherwise, all sub columns should be left nullable
					 */
					final boolean canSetNotNullOnColumns = activateNotNull && hasNotNull;
					applyDDL( prefix + propertyDesc.getPropertyName() + ".",
							persistentClass, componentClass, factory, groups,
							canSetNotNullOnColumns
					);
				}
				//FIXME add collection of components
			}
		}
	}

	private static boolean applyConstraints(Set<ConstraintDescriptor<?>> constraintDescriptors,
										 Property property,
										 PropertyDescriptor propertyDesc,
										 Set<Class<?>> groups, boolean canApplyNotNull) {
		boolean hasNotNull = false;
		for (ConstraintDescriptor<?> descriptor : constraintDescriptors) {
			if ( groups != null && Collections.disjoint( descriptor.getGroups(), groups) ) continue;

			if ( canApplyNotNull ) {
				hasNotNull = hasNotNull || applyNotNull( property, descriptor );
			}
			applyDigits( property, descriptor );
			applySize( property, descriptor, propertyDesc );
			applyMin( property, descriptor );
			applyMax( property, descriptor );

			//pass an empty set as composing constraints inherit the main constraint and thus are matching already
			hasNotNull = hasNotNull || applyConstraints(
					descriptor.getComposingConstraints(),
					property, propertyDesc, null,
					canApplyNotNull );
		}
		return hasNotNull;
	}

	private static void applyMin(Property property, ConstraintDescriptor<?> descriptor) {
		if ( Min.class.equals( descriptor.getAnnotation().annotationType() ) ) {
			@SuppressWarnings( "unchecked" )
			ConstraintDescriptor<Min> minConstraint = (ConstraintDescriptor<Min>) descriptor;
			long min = minConstraint.getAnnotation().value();
			Column col = (Column) property.getColumnIterator().next();
			col.setCheckConstraint( col.getName() + ">=" + min );
		}
	}

	private static void applyMax(Property property, ConstraintDescriptor<?> descriptor) {
		if ( Max.class.equals( descriptor.getAnnotation().annotationType() ) ) {
			@SuppressWarnings( "unchecked" )
			ConstraintDescriptor<Max> maxConstraint = (ConstraintDescriptor<Max>) descriptor;
			long max = maxConstraint.getAnnotation().value();
			Column col = (Column) property.getColumnIterator().next();
			col.setCheckConstraint( col.getName() + "<=" + max );
		}
	}

	private static boolean applyNotNull(Property property, ConstraintDescriptor<?> descriptor) {
		boolean hasNotNull = false;
		if ( NotNull.class.equals( descriptor.getAnnotation().annotationType() ) ) {
			if ( ! ( property.getPersistentClass() instanceof SingleTableSubclass ) ) {
				//single table should not be forced to null
				if ( !property.isComposite() ) { //composite should not add not-null on all columns
					@SuppressWarnings( "unchecked" )
					Iterator<Column> iter = (Iterator<Column>) property.getColumnIterator();
					while ( iter.hasNext() ) {
						iter.next().setNullable( false );
						hasNotNull = true;
					}
				}
			}
			hasNotNull = true;
		}
		return hasNotNull;
	}

	private static void applyDigits(Property property, ConstraintDescriptor<?> descriptor) {
		if ( Digits.class.equals( descriptor.getAnnotation().annotationType() ) ) {
			@SuppressWarnings( "unchecked" )
			ConstraintDescriptor<Digits> digitsConstraint = (ConstraintDescriptor<Digits>) descriptor;
			int integerDigits = digitsConstraint.getAnnotation().integer();
			int fractionalDigits = digitsConstraint.getAnnotation().fraction();
			Column col = (Column) property.getColumnIterator().next();
			col.setPrecision( integerDigits + fractionalDigits );
			col.setScale( fractionalDigits );
		}
	}

	private static void applySize(Property property, ConstraintDescriptor<?> descriptor, PropertyDescriptor propertyDesc) {
		if ( Size.class.equals( descriptor.getAnnotation().annotationType() )
				&& String.class.equals( propertyDesc.getElementClass() ) ) {
			@SuppressWarnings( "unchecked" )
			ConstraintDescriptor<Size> sizeConstraint = (ConstraintDescriptor<Size>) descriptor;
			int max = sizeConstraint.getAnnotation().max();
			Column col = (Column) property.getColumnIterator().next();
			if ( max < Integer.MAX_VALUE ) col.setLength( max );
		}
	}

	/**
	 * Retrieve the property by path in a recursive way, including IndentifierProperty in the loop
	 * If propertyName is null or empty, the IdentifierProperty is returned
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
					String element = (String) st.nextElement();
					if ( property == null ) {
						property = associatedClass.getProperty( element );
					}
					else {
						if ( ! property.isComposite() ) return null;
						property = ( ( Component ) property.getValue() ).getProperty( element );
					}
				}
			}
		}
		catch ( MappingException e) {
			try {
				//if we do not find it try to check the identifier mapper
				if ( associatedClass.getIdentifierMapper() == null ) return null;
				StringTokenizer st = new StringTokenizer( propertyName, ".", false );
				while ( st.hasMoreElements() ) {
					String element = (String) st.nextElement();
					if ( property == null ) {
						property = associatedClass.getIdentifierMapper().getProperty( element );
					}
					else {
						if ( ! property.isComposite() ) return null;
						property = ( (Component) property.getValue() ).getProperty( element );
					}
				}
			}
			catch (MappingException ee) {
				return null;
			}
		}
		return property;
	}

	private static ValidatorFactory getValidatorFactory(Map<Object, Object> properties) {
		ValidatorFactory factory = null;
		if ( properties != null ) {
			Object unsafeProperty = properties.get( FACTORY_PROPERTY );
			if (unsafeProperty != null) {
				try {
					factory = ValidatorFactory.class.cast( unsafeProperty );
				}
				catch ( ClassCastException e ) {
					throw new HibernateException( "Property " + FACTORY_PROPERTY
							+ " should contain an object of type " + ValidatorFactory.class.getName() );
				}
			}
		}
		if (factory == null) {
			try {
				factory = Validation.buildDefaultValidatorFactory();
			}
			catch ( Exception e ) {
				throw new HibernateException( "Unable to build the default ValidatorFactory", e);
			}
		}
		return factory;
	}

}
