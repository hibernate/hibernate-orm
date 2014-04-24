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
import java.util.List;
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

import org.hibernate.EntityMode;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metamodel.spi.binding.AbstractSingularAssociationAttributeBinding;
import org.hibernate.metamodel.spi.binding.AbstractSingularAttributeBinding;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.EmbeddedAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.domain.Attribute;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.Column;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
class TypeSafeActivator {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( TypeSafeActivator.class );

	/**
	 * Used to validate a supplied ValidatorFactory instance as being castable to ValidatorFactory.
	 *
	 * @param object The supplied ValidatorFactory instance.
	 */
	@SuppressWarnings({ "UnusedDeclaration" })
	public static void validateSuppliedFactory(Object object) {
		if ( !ValidatorFactory.class.isInstance( object ) ) {
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
		catch ( IntegrationException e ) {
			if ( activationContext.getValidationModes().contains( ValidationMode.CALLBACK ) ) {
				throw new IntegrationException(
						"Bean Validation provider was not available, but 'callback' validation was requested",
						e
				);
			}
			if ( activationContext.getValidationModes().contains( ValidationMode.DDL ) ) {
				throw new IntegrationException(
						"Bean Validation provider was not available, but 'ddl' validation was requested",
						e
				);
			}

			LOG.debug( "Unable to acquire Bean Validation ValidatorFactory, skipping activation" );
			return;
		}

		applyRelationalConstraints( factory, activationContext );

		applyCallbackListeners( factory, activationContext );
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	public static void applyCallbackListeners(ValidatorFactory validatorFactory, ActivationContext activationContext) {
		final Set<ValidationMode> modes = activationContext.getValidationModes();
		if ( !( modes.contains( ValidationMode.CALLBACK ) || modes.contains( ValidationMode.AUTO ) ) ) {
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

	private static void applyRelationalConstraints(ValidatorFactory factory, ActivationContext activationContext) {
		final Map properties = activationContext.getSettings();
		if ( !ConfigurationHelper.getBoolean( BeanValidationIntegrator.APPLY_CONSTRAINTS, properties, true ) ) {
			LOG.debug( "Skipping application of relational constraints from legacy Hibernate Validator" );
			return;
		}

		final Set<ValidationMode> modes = activationContext.getValidationModes();
		if ( !( modes.contains( ValidationMode.DDL ) || modes.contains( ValidationMode.AUTO ) ) ) {
			return;
		}

		final Dialect dialect = activationContext.getServiceRegistry().getService( JdbcServices.class ).getDialect();

		Class<?>[] groupsArray = new GroupsPerOperation( properties ).get( GroupsPerOperation.Operation.DDL );
		Set<Class<?>> groups = new HashSet<Class<?>>( Arrays.asList( groupsArray ) );

		for ( EntityBinding entityBinding : activationContext.getMetadata().getEntityBindings() ) {
			final String className = entityBinding.getEntity().getDescriptor().getName().toString();

			if ( entityBinding.getHierarchyDetails().getEntityMode() != EntityMode.POJO ) {
				continue;
			}

			final ClassLoaderService classLoaderService = activationContext.getServiceRegistry()
					.getService( ClassLoaderService.class );
			final Class<?> clazz = classLoaderService.classForName( className );
			try {
				applyDDL( "", entityBinding, clazz, factory, groups, true, dialect, classLoaderService );
			}
			catch ( Exception e ) {
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
			Dialect dialect, ClassLoaderService classLoaderService) {
		final BeanDescriptor descriptor = factory.getValidator().getConstraintsForClass( clazz );

		// no bean level constraints can be applied, just iterate the properties
		for ( PropertyDescriptor propertyDescriptor : descriptor.getConstrainedProperties() ) {
			AttributeBinding attributeBinding = findPropertyByName(
					entityBinding,
					prefix + propertyDescriptor.getPropertyName()
			);
			if ( attributeBinding == null ) {
				continue;
			}

			boolean hasNotNull;
			hasNotNull = applyConstraints(
					propertyDescriptor.getConstraintDescriptors(),
					attributeBinding,
					propertyDescriptor,
					groups,
					activateNotNull,
					dialect
			);

			if ( propertyDescriptor.isCascaded() ) {
				// if it is a composite, visit its attributes
				final Attribute attribute = attributeBinding.getAttribute();
				if ( attribute.isSingular() ) {
					final SingularAttribute singularAttribute = (SingularAttribute) attribute;
					if ( singularAttribute.getSingularAttributeType().isAggregate() ) {
						final Class<?> componentClass = classLoaderService.classForName(
								singularAttribute.getSingularAttributeType().getDescriptor().getName().toString()
						);
						final boolean canSetNotNullOnColumns = activateNotNull && hasNotNull;
						applyDDL(
								prefix + propertyDescriptor.getPropertyName() + ".",
								entityBinding,
								componentClass,
								factory,
								groups,
								canSetNotNullOnColumns,
								dialect,
								classLoaderService
						);
					}
				}
			}
		}
	}

	private static boolean applyConstraints(
			Set<ConstraintDescriptor<?>> constraintDescriptors,
			AttributeBinding attributeBinding,
			PropertyDescriptor propertyDescriptor,
			Set<Class<?>> groups,
			boolean canApplyNotNull,
			Dialect dialect) {
		boolean hasNotNull = false;
		for ( ConstraintDescriptor<?> descriptor : constraintDescriptors ) {
			if ( groups != null && Collections.disjoint( descriptor.getGroups(), groups ) ) {
				continue;
			}

			if ( canApplyNotNull ) {
				hasNotNull = hasNotNull || applyNotNull( attributeBinding, descriptor );
			}

			// apply bean validation specific constraints
			applyDigits( attributeBinding, descriptor );
			applySize( attributeBinding, descriptor, propertyDescriptor );
			applyMin( attributeBinding, descriptor, dialect );
			applyMax( attributeBinding, descriptor, dialect );

			// apply hibernate validator specific constraints - we cannot import any HV specific classes though!
			// no need to check explicitly for @Range. @Range is a composed constraint using @Min and @Max which
			// will be taken care later
			applyLength( attributeBinding, descriptor );

			// pass an empty set as composing constraints inherit the main constraint and thus are matching already
			hasNotNull = hasNotNull || applyConstraints(
					descriptor.getComposingConstraints(),
					attributeBinding, propertyDescriptor, null,
					canApplyNotNull,
					dialect
			);
		}
		return hasNotNull;
	}

	private static void applySQLCheck(Column column, String checkConstraint) {
		String existingCheck = column.getCheckCondition();
		// need to check whether the new check is already part of the existing check, because applyDDL can be called
		// multiple times
		if ( StringHelper.isNotEmpty( existingCheck ) && !existingCheck.contains( checkConstraint ) ) {
			checkConstraint = column.getCheckCondition() + " AND " + checkConstraint;
		}
		column.setCheckCondition( checkConstraint );
	}

	private static boolean applyNotNull(AttributeBinding attributeBinding, ConstraintDescriptor<?> descriptor) {
		if ( !NotNull.class.equals( descriptor.getAnnotation().annotationType() ) ) {
			return false;
		}

		if ( InheritanceType.SINGLE_TABLE.equals(
				attributeBinding.getContainer()
						.seekEntityBinding()
						.getHierarchyDetails()
						.getInheritanceType()
		) ) {
			return false;
		}

		List<RelationalValueBinding> relationalValueBindings = Collections.emptyList();
		if ( attributeBinding instanceof BasicAttributeBinding ) {
			BasicAttributeBinding basicBinding = (BasicAttributeBinding) attributeBinding;
			relationalValueBindings = basicBinding.getRelationalValueBindings();
		}

		if ( attributeBinding instanceof AbstractSingularAssociationAttributeBinding ) {
			AbstractSingularAssociationAttributeBinding singularAttributeBinding = (AbstractSingularAssociationAttributeBinding) attributeBinding;
			relationalValueBindings = singularAttributeBinding.getRelationalValueBindings();
		}

		if ( attributeBinding instanceof AbstractSingularAttributeBinding ) {
			AbstractSingularAttributeBinding singularAttributeBinding = (AbstractSingularAttributeBinding) attributeBinding;
			relationalValueBindings = singularAttributeBinding.getRelationalValueBindings();
		}

		for ( RelationalValueBinding relationalValueBinding : relationalValueBindings ) {
			if ( relationalValueBinding.getValue() instanceof Column ) {
				Column column = (Column) relationalValueBinding.getValue();
				column.setNullable( false );
			}
		}

		return true;
	}

	private static void applyMin(AttributeBinding attributeBinding, ConstraintDescriptor<?> descriptor, Dialect dialect) {
		if ( !Min.class.equals( descriptor.getAnnotation().annotationType() ) ) {
			return;
		}

		long min = (Long) descriptor.getAttributes().get( "value" );
		Column column = getSingleColumn( attributeBinding );
		String checkConstraint = column.getColumnName().getText( dialect ) + ">=" + min;
		applySQLCheck( column, checkConstraint );
	}

	private static void applyMax(AttributeBinding attributeBinding, ConstraintDescriptor<?> descriptor, Dialect dialect) {
		if ( !Max.class.equals( descriptor.getAnnotation().annotationType() ) ) {
			return;
		}

		long max = (Long) descriptor.getAttributes().get( "value" );
		Column column = getSingleColumn( attributeBinding );
		String checkConstraint = column.getColumnName().getText( dialect ) + "<=" + max;
		applySQLCheck( column, checkConstraint );
	}

	private static void applySize(AttributeBinding attributeBinding, ConstraintDescriptor<?> descriptor, PropertyDescriptor propertyDescriptor) {
		if ( !( Size.class.equals( descriptor.getAnnotation().annotationType() )
				&& String.class.equals( propertyDescriptor.getElementClass() ) ) ) {
			return;
		}

		int max = (Integer) descriptor.getAttributes().get( "max" );
		Column column = getSingleColumn( attributeBinding );
		if ( max < Integer.MAX_VALUE ) {
			column.setSize( org.hibernate.metamodel.spi.relational.Size.length( max ) );
		}
	}

	private static void applyLength(AttributeBinding attributeBinding, ConstraintDescriptor<?> descriptor) {
		if ( !"org.hibernate.validator.constraints.Length".equals(
				descriptor.getAnnotation().annotationType().getName()
		) ) {
			return;
		}

		int max = (Integer) descriptor.getAttributes().get( "max" );
		Column column = getSingleColumn( attributeBinding );
		if ( max < Integer.MAX_VALUE ) {
			column.setSize( org.hibernate.metamodel.spi.relational.Size.length( max ) );
		}
	}

	private static void applyDigits(AttributeBinding attributeBinding, ConstraintDescriptor<?> descriptor) {
		if ( !Digits.class.equals( descriptor.getAnnotation().annotationType() ) ) {
			return;
		}
		@SuppressWarnings("unchecked")
		ConstraintDescriptor<Digits> digitsConstraint = (ConstraintDescriptor<Digits>) descriptor;
		int integerDigits = digitsConstraint.getAnnotation().integer();
		int fractionalDigits = digitsConstraint.getAnnotation().fraction();

		Column column = getSingleColumn( attributeBinding );
		org.hibernate.metamodel.spi.relational.Size size = org.hibernate.metamodel.spi.relational.Size.precision(
				integerDigits + fractionalDigits,
				fractionalDigits
		);
		column.setSize( size );
	}

	/**
	 * Returns the {@code AttributeBinding} for the attribute/property specified by given path.
	 *
	 * @param entityBinding the root entity binding from which to start the search for the property
	 * @param propertyPath the property path
	 *
	 * @return Returns the {@code AttributeBinding} for the attribute/property specified by given path. If
	 * {@code propertyPath} is {@code null} or empty, the id attribute binding is returned.
	 */
	private static AttributeBinding findPropertyByName(EntityBinding entityBinding, String propertyPath) {
		final AttributeBinding idAttributeBinding;

		final EntityIdentifier idInfo = entityBinding.getHierarchyDetails().getEntityIdentifier();
		if ( idInfo.getNature() == EntityIdentifierNature.NON_AGGREGATED_COMPOSITE ) {
			idAttributeBinding = null;
		}
		else {
			final EntityIdentifier.AttributeBasedIdentifierBinding identifierBinding =
					(EntityIdentifier.AttributeBasedIdentifierBinding) idInfo.getEntityIdentifierBinding();
			idAttributeBinding = identifierBinding.getAttributeBinding();
		}

		final String idAttributeName = idAttributeBinding == null ? null : idAttributeBinding.getAttribute().getName();

		if ( propertyPath == null || propertyPath.length() == 0 || propertyPath.equals( idAttributeName ) ) {
			//default to id
			return idAttributeBinding;
		}

		AttributeBinding attributeBinding = null;
		StringTokenizer tokenizer = new StringTokenizer( propertyPath, ".", false );
		while ( tokenizer.hasMoreElements() ) {
			String element = (String) tokenizer.nextElement();
			if ( attributeBinding == null ) {
				attributeBinding = entityBinding.locateAttributeBinding( element );
			}
			else {
				if ( !isComposite( attributeBinding ) ) {
					return null;
				}
				EmbeddedAttributeBinding embeddedAttributeBinding = (EmbeddedAttributeBinding) attributeBinding;
				attributeBinding = embeddedAttributeBinding.getEmbeddableBinding().locateAttributeBinding( element );
			}
		}
		return attributeBinding;
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
		final Object reference = activationContext.getSessionFactory()
				.getSessionFactoryOptions()
				.getValidatorFactoryReference();
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

	private static Column getSingleColumn(AttributeBinding attributeBinding) {
		BasicAttributeBinding basicBinding = (BasicAttributeBinding) attributeBinding;
		List<RelationalValueBinding> relationalValueBindings = basicBinding.getRelationalValueBindings();
		if ( relationalValueBindings.size() > 1 ) {
			throw new IntegrationException(
					"Unexpected number of relational columns for attribute "
							+ attributeBinding.getAttribute().getName()
			);
		}
		for ( RelationalValueBinding relationalValueBinding : relationalValueBindings ) {
			if ( relationalValueBinding.getValue() instanceof Column ) {
				return (Column) relationalValueBinding.getValue();
			}
		}
		return null;
	}
}
