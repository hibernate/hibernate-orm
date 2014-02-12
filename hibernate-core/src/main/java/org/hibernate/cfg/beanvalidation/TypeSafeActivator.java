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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.PropertyDescriptor;

import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.domain.Attribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;

import org.jboss.logging.Logger;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
class TypeSafeActivator {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, TypeSafeActivator.class.getName());

	/**
	 * Used to validate a supplied ValidatorFactory instance as being castable to ValidatorFactory.
	 *
	 * @param object The supplied ValidatorFactory instance.
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	public static void validateSuppliedFactory(Object object) {
		if ( ! ValidatorFactory.class.isInstance( object ) ) {
			throw new IntegrationException(
					"Given object was not an instance of " + ValidatorFactory.class.getName()
							+ "[" + object.getClass().getName() + "]"
			);
		}
	}

	@SuppressWarnings("UnusedDeclaration")
	public static void activate(ActivationContext activationContext) {
		final Map properties = activationContext.getSettings();
		final ValidatorFactory factory;
		try {
			factory = getValidatorFactory( activationContext );
		}
		catch (IntegrationException e) {
			if ( activationContext.getValidationModes().contains( ValidationMode.CALLBACK ) ) {
				throw new IntegrationException( "Bean Validation provider was not available, but 'callback' validation was requested", e );
			}
			if ( activationContext.getValidationModes().contains( ValidationMode.DDL ) ) {
				throw new IntegrationException( "Bean Validation provider was not available, but 'ddl' validation was requested", e );
			}

			LOG.debug( "Unable to acquire Bean Validation ValidatorFactory, skipping activation" );
			return;
		}

		applyRelationalConstraints( factory, activationContext );

		applyCallbackListeners( factory, activationContext );
	}

	@SuppressWarnings( {"UnusedDeclaration"})
	public static void applyCallbackListeners(ValidatorFactory validatorFactory, ActivationContext activationContext) {
		final Set<ValidationMode> modes = activationContext.getValidationModes();
		if ( ! ( modes.contains( ValidationMode.CALLBACK ) || modes.contains( ValidationMode.AUTO ) ) ) {
			return;
		}

		// de-activate not-null tracking at the core level when Bean Validation is present unless the user explicitly
		// asks for it
		if ( activationContext.getSettings().get( Environment.CHECK_NULLABILITY ) == null ) {
			activationContext.getSessionFactory().getSettings().setCheckNullability( false );
		}

		final BeanValidationEventListener listener = new BeanValidationEventListener(
				validatorFactory,
				activationContext.getSettings()
		);

		final EventListenerRegistry listenerRegistry = activationContext.getServiceRegistry()
				.getService( EventListenerRegistry.class );

		listenerRegistry.addDuplicationStrategy( DuplicationStrategyImpl.INSTANCE );

		listenerRegistry.appendListeners( EventType.PRE_INSERT, listener );
		listenerRegistry.appendListeners( EventType.PRE_UPDATE, listener );
		listenerRegistry.appendListeners( EventType.PRE_DELETE, listener );

		listener.initialize( activationContext.getSettings() );
	}

	@SuppressWarnings({"unchecked", "UnusedParameters"})
	private static void applyRelationalConstraints(ValidatorFactory factory, ActivationContext activationContext) {
		final Map properties = activationContext.getSettings();
		if ( ! ConfigurationHelper.getBoolean( BeanValidationIntegrator.APPLY_CONSTRAINTS, properties, true ) ){
			LOG.debug( "Skipping application of relational constraints from legacy Hibernate Validator" );
			return;
		}

		final Set<ValidationMode> modes = activationContext.getValidationModes();
		if ( ! ( modes.contains( ValidationMode.DDL ) || modes.contains( ValidationMode.AUTO ) ) ) {
			return;
		}

		final Dialect dialect = activationContext.getServiceRegistry().getService( JdbcServices.class ).getDialect();

		Class<?>[] groupsArray = new GroupsPerOperation( properties ).get( GroupsPerOperation.Operation.DDL );
		Set<Class<?>> groups = new HashSet<Class<?>>( Arrays.asList( groupsArray ) );

		// Hardy : I started working on this as part of a big metamodel clean up I am doing, but this got to be too
		// 	much of a rabbit hole.  - Steve
		for ( EntityBinding entityBinding : activationContext.getMetadata().getEntityBindings() ) {
			final String className = entityBinding.getEntity().getClassReference().getName();

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
				applyDDL( "", entityBinding, clazz, factory, groups, true, dialect );
			}
			catch (Exception e) {
				LOG.unableToApplyConstraints( className, e );
			}
		}
	}

	private static void applyDDL(
			String prefix,
			EntityBinding entityBinding,
			Class<?> clazz,
			ValidatorFactory factory,
			Set<Class<?>> groups,
			boolean activateNotNull,
			Dialect dialect) {
		final BeanDescriptor descriptor = factory.getValidator().getConstraintsForClass( clazz );
		//no bean level constraints can be applied, go to the properties

		for ( PropertyDescriptor propertyDesc : descriptor.getConstrainedProperties() ) {
			AttributeBinding attributeBinding = findPropertyByName(
					entityBinding,
					prefix + propertyDesc.getPropertyName()
			);
			boolean hasNotNull;
			if ( attributeBinding != null ) {
				hasNotNull = applyConstraints(
						propertyDesc.getConstraintDescriptors(),
						attributeBinding,
						propertyDesc,
						groups,
						activateNotNull,
						dialect
				);

				if ( propertyDesc.isCascaded() ) {
					// if it is a composite, visit its attributes
					final Attribute attribute = attributeBinding.getAttribute();
					if ( attribute.isSingular() ) {
						final SingularAttribute singularAttribute = (SingularAttribute) attribute;
						if ( singularAttribute.getSingularAttributeType().isAggregate() ) {
							final Class componentClass =
									singularAttribute.getSingularAttributeType().getClassReference().getResolvedClass();
							final boolean canSetNotNullOnColumns = activateNotNull && hasNotNull;
							applyDDL(
									prefix + propertyDesc.getPropertyName() + ".",
									entityBinding,
									componentClass,
									factory,
									groups,
									canSetNotNullOnColumns,
									dialect
							);
						}
					}
				}
			}
		}
	}

	private static boolean applyConstraints(
			Set<ConstraintDescriptor<?>> constraintDescriptors,
			AttributeBinding property,
			PropertyDescriptor propertyDesc,
			Set<Class<?>> groups,
			boolean canApplyNotNull,
			Dialect dialect) {
		boolean hasNotNull = false;
		for ( ConstraintDescriptor<?> descriptor : constraintDescriptors ) {
			if ( groups != null && Collections.disjoint( descriptor.getGroups(), groups ) ) {
				continue;
			}

			// todo : Hardy - here are the rabbit holes...
// first...
//			if ( canApplyNotNull ) {
//				hasNotNull = hasNotNull || applyNotNull( property, descriptor );
//			}

			// apply bean validation specific constraints
// second...
//			applyDigits( property, descriptor );
//			applySize( property, descriptor, propertyDesc );
//			applyMin( property, descriptor, dialect );
//			applyMax( property, descriptor, dialect );

			// apply hibernate validator specific constraints - we cannot import any HV specific classes though!
			// no need to check explicitly for @Range. @Range is a composed constraint using @Min and @Max which
			// will be taken care later
// third
//			applyLength( property, descriptor, propertyDesc );

			// pass an empty set as composing constraints inherit the main constraint and thus are matching already
			hasNotNull = hasNotNull || applyConstraints(
					descriptor.getComposingConstraints(),
					property, propertyDesc, null,
					canApplyNotNull,
                    dialect
			);
		}
		return hasNotNull;
	}

	private static void applyMin(Property property, ConstraintDescriptor<?> descriptor, Dialect dialect) {
		if ( Min.class.equals( descriptor.getAnnotation().annotationType() ) ) {
			@SuppressWarnings("unchecked")
			ConstraintDescriptor<Min> minConstraint = (ConstraintDescriptor<Min>) descriptor;
			long min = minConstraint.getAnnotation().value();

			Column col = (Column) property.getColumnIterator().next();
			String checkConstraint = col.getQuotedName(dialect) + ">=" + min;
			applySQLCheck( col, checkConstraint );
		}
	}

	private static void applyMax(Property property, ConstraintDescriptor<?> descriptor, Dialect dialect) {
		if ( Max.class.equals( descriptor.getAnnotation().annotationType() ) ) {
			@SuppressWarnings("unchecked")
			ConstraintDescriptor<Max> maxConstraint = (ConstraintDescriptor<Max>) descriptor;
			long max = maxConstraint.getAnnotation().value();
			Column col = (Column) property.getColumnIterator().next();
			String checkConstraint = col.getQuotedName(dialect) + "<=" + max;
			applySQLCheck( col, checkConstraint );
		}
	}

	private static void applySQLCheck(Column col, String checkConstraint) {
		String existingCheck = col.getCheckConstraint();
		// need to check whether the new check is already part of the existing check, because applyDDL can be called
		// multiple times
		if ( StringHelper.isNotEmpty( existingCheck ) && !existingCheck.contains( checkConstraint ) ) {
			checkConstraint = col.getCheckConstraint() + " AND " + checkConstraint;
		}
		col.setCheckConstraint( checkConstraint );
	}

	private static boolean applyNotNull(Property property, ConstraintDescriptor<?> descriptor) {
		boolean hasNotNull = false;
		if ( NotNull.class.equals( descriptor.getAnnotation().annotationType() ) ) {
			// single table inheritance should not be forced to null due to shared state
			if ( !( property.getPersistentClass() instanceof SingleTableSubclass ) ) {
				//composite should not add not-null on all columns
				if ( !property.isComposite() ) {
					final Iterator<Selectable> iter = property.getColumnIterator();
					while ( iter.hasNext() ) {
						final Selectable selectable = iter.next();
						if ( Column.class.isInstance( selectable ) ) {
							Column.class.cast( selectable ).setNullable( false );
						}
						else {
							LOG.debugf(
									"@NotNull was applied to attribute [%s] which is defined (at least partially) " +
											"by formula(s); formula portions will be skipped",
									property.getName()
							);
						}
					}
				}
			}
			hasNotNull = true;
		}
		return hasNotNull;
	}

	private static void applyDigits(Property property, ConstraintDescriptor<?> descriptor) {
		if ( Digits.class.equals( descriptor.getAnnotation().annotationType() ) ) {
			@SuppressWarnings("unchecked")
			ConstraintDescriptor<Digits> digitsConstraint = (ConstraintDescriptor<Digits>) descriptor;
			int integerDigits = digitsConstraint.getAnnotation().integer();
			int fractionalDigits = digitsConstraint.getAnnotation().fraction();
			Column col = (Column) property.getColumnIterator().next();
			col.setPrecision( integerDigits + fractionalDigits );
			col.setScale( fractionalDigits );
		}
	}

	private static void applySize(Property property, ConstraintDescriptor<?> descriptor, PropertyDescriptor propertyDescriptor) {
		if ( Size.class.equals( descriptor.getAnnotation().annotationType() )
				&& String.class.equals( propertyDescriptor.getElementClass() ) ) {
			@SuppressWarnings("unchecked")
			ConstraintDescriptor<Size> sizeConstraint = (ConstraintDescriptor<Size>) descriptor;
			int max = sizeConstraint.getAnnotation().max();
			Column col = (Column) property.getColumnIterator().next();
			if ( max < Integer.MAX_VALUE ) {
				col.setLength( max );
			}
		}
	}

	private static void applyLength(Property property, ConstraintDescriptor<?> descriptor, PropertyDescriptor propertyDescriptor) {
		if ( "org.hibernate.validator.constraints.Length".equals(
				descriptor.getAnnotation().annotationType().getName()
		)
				&& String.class.equals( propertyDescriptor.getElementClass() ) ) {
			@SuppressWarnings("unchecked")
			int max = (Integer) descriptor.getAttributes().get( "max" );
			Column col = (Column) property.getColumnIterator().next();
			if ( max < Integer.MAX_VALUE ) {
				col.setLength( max );
			}
		}
	}

	private static AttributeBinding findPropertyByName(EntityBinding entityBinding, String propertyName) {
		// Returns the property by path in a recursive way, including IdentifierProperty in the loop
		// if propertyName is null.  If propertyName is null or empty, the IdentifierProperty is returned
		final AttributeBinding idAttributeBinding = entityBinding.getHierarchyDetails().getEntityIdentifier().getAttributeBinding();
		final String idAttributeName = idAttributeBinding == null ? null : idAttributeBinding.getAttribute().getName();

		AttributeBinding property = null;
		try {
			if ( propertyName == null
					|| propertyName.length() == 0
					|| propertyName.equals( idAttributeName ) ) {
				//default to id
				property = idAttributeBinding;
			}
			else {
				if ( propertyName.indexOf( idAttributeName + "." ) == 0 ) {
					property = idAttributeBinding;
					propertyName = propertyName.substring( idAttributeName.length() + 1 );
				}
				StringTokenizer st = new StringTokenizer( propertyName, ".", false );
				while ( st.hasMoreElements() ) {
					String element = (String) st.nextElement();
					if ( property == null ) {
						property = entityBinding.locateAttributeBinding( element );
					}
					else {
						if ( !isComposite( property ) ) {
							return null;
						}
						// todo : Hardy - not sure exactly what to do here...
						//property = ( (Component) property.getValue() ).getProperty( element );
					}
				}
			}
		}
		catch ( MappingException e ) {
			// todo : Hardy - nor here...
//			try {
//				//if we do not find it try to check the identifier mapper
//				if ( associatedClass.getIdentifierMapper() == null ) {
//					return null;
//				}
//				StringTokenizer st = new StringTokenizer( propertyName, ".", false );
//				while ( st.hasMoreElements() ) {
//					String element = (String) st.nextElement();
//					if ( property == null ) {
//						property = associatedClass.getIdentifierMapper().getProperty( element );
//					}
//					else {
//						if ( !property.isComposite() ) {
//							return null;
//						}
//						property = ( (Component) property.getValue() ).getProperty( element );
//					}
//				}
//			}
//			catch ( MappingException ee ) {
//				return null;
//			}
		}
		return property;
	}

	private static boolean isComposite(AttributeBinding property) {
		if ( property.getAttribute().isSingular() ) {
			final SingularAttribute singularAttribute = (SingularAttribute) property.getAttribute();
			return singularAttribute.getSingularAttributeType().isAggregate();
		}

		return false;
	}

	private static ValidatorFactory getValidatorFactory(ActivationContext activationContext) {
		// first look for an explicitly passed ValidatorFactory
		final Object reference = activationContext.getSessionFactory().getSessionFactoryOptions().getValidatorFactoryReference();
		if ( reference != null ) {
			try {
				return ValidatorFactory.class.cast( reference );
			}
			catch ( ClassCastException e ) {
				throw new IntegrationException(
						"Passed ValidatorFactory was not of correct type; expected " + ValidatorFactory.class.getName() +
								", but found " + reference.getClass().getName()
				);
			}
		}

		try {
			return Validation.buildDefaultValidatorFactory();
		}
		catch ( Exception e ) {
			throw new IntegrationException( "Unable to build the default ValidatorFactory", e );
		}
	}
}
