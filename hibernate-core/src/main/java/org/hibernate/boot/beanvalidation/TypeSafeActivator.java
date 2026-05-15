/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.beanvalidation;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.validation.NoProviderFoundException;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.AssertionFailure;
import org.hibernate.boot.beanvalidation.GroupsPerOperation.Operation;
import org.hibernate.boot.internal.ClassLoaderAccessImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventType;
import org.hibernate.mapping.CheckConstraint;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SingleTableSubclass;

import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.metadata.ConstraintDescriptor;
import jakarta.validation.metadata.PropertyDescriptor;

import static java.util.Collections.disjoint;
import static org.hibernate.boot.beanvalidation.BeanValidationIntegrator.APPLY_CONSTRAINTS;
import static org.hibernate.boot.beanvalidation.BeanValidationLogger.BEAN_VALIDATION_LOGGER;
import static org.hibernate.boot.beanvalidation.GroupsPerOperation.buildGroupsForOperation;
import static org.hibernate.boot.model.internal.BinderHelper.findPropertyByName;
import static org.hibernate.cfg.ValidationSettings.CHECK_NULLABILITY;
import static org.hibernate.cfg.ValidationSettings.JAKARTA_VALIDATION_FACTORY;
import static org.hibernate.cfg.ValidationSettings.JPA_VALIDATION_FACTORY;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/**
 * Sets up Bean Validation {@linkplain BeanValidationEventListener event listener} and DDL-based constraints.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
class TypeSafeActivator {


	/**
	 * Used to validate a supplied ValidatorFactory instance as being castable to ValidatorFactory.
	 *
	 * @param object The supplied ValidatorFactory instance.
	 */
	@SuppressWarnings("unused")
	public static void validateSuppliedFactory(Object object) {
		if ( !(object instanceof ValidatorFactory) ) {
			throw new IntegrationException( "Given object was not an instance of " + ValidatorFactory.class.getName()
							+ " [" + object.getClass().getName() + "]" );
		}
	}

	@SuppressWarnings("unused")
	public static void activate(ActivationContext context) {
		final ValidatorFactory factory;
		try {
			factory = getValidatorFactory( context );
		}
		catch (IntegrationException exception) {
			final var validationModes = context.getValidationModes();
			if ( validationModes.contains( ValidationMode.CALLBACK ) ) {
				throw new IntegrationException( "Jakarta Validation provider was not available, but 'callback' validation mode was requested", exception );
			}
			else if ( validationModes.contains( ValidationMode.DDL ) ) {
				throw new IntegrationException( "Jakarta Validation provider was not available, but 'ddl' validation mode was requested", exception );
			}
			else {
				if ( exception.getCause() instanceof NoProviderFoundException ) {
					// all good, we are looking at the ValidationMode.AUTO, and there are no providers available.
					// Hence, we just don't enable the Jakarta Validation integration:
					BEAN_VALIDATION_LOGGER.validationFactorySkipped();
					return;
				}
				else {
					// There is a Jakarta Validation provider, but it failed to bootstrap the factory for some reason,
					// we should fail and let the user deal with it:
					throw exception;
				}
			}
		}

		applyRelationalConstraints( factory, context );
		applyCallbackListeners( factory, context );
	}

	public static void applyCallbackListeners(ValidatorFactory validatorFactory, ActivationContext context) {
		if ( isValidationEnabled( context ) ) {
			disableNullabilityChecking( context );
			setupListener( validatorFactory, context.getServiceRegistry(), context.getSessionFactory() );
		}
	}

	private static boolean isValidationEnabled(ActivationContext context) {
		final var modes = context.getValidationModes();
		return modes.contains( ValidationMode.CALLBACK )
			|| modes.contains( ValidationMode.AUTO );
	}

	/**
	 * Deactivate not-null tracking at the core level when Bean Validation
	 * is present, unless the user explicitly asks for it.
	 */
	private static void disableNullabilityChecking(ActivationContext context) {
		if ( isCheckNullabilityExplicit( context ) ) {
			// no explicit setting, so disable null checking
			context.getSessionFactory().getSessionFactoryOptions().setCheckNullability( false );
		}
	}

	private static boolean isCheckNullabilityExplicit(ActivationContext context) {
		return context.getServiceRegistry().requireService( ConfigurationService.class )
				.getSettings().get( CHECK_NULLABILITY ) == null;
	}

	private static void setupListener(
			ValidatorFactory validatorFactory,
			SessionFactoryServiceRegistry serviceRegistry,
			SessionFactoryImplementor sessionFactory) {
		final var listener =
				new BeanValidationEventListener( validatorFactory,
						serviceRegistry.requireService( ConfigurationService.class ).getSettings(),
						serviceRegistry.requireService( ClassLoaderService.class ) );
		final var listenerRegistry = sessionFactory.getEventListenerRegistry();
		listenerRegistry.addDuplicationStrategy( DuplicationStrategyImpl.INSTANCE );
		listenerRegistry.appendListeners( EventType.PRE_INSERT, listener );
		listenerRegistry.appendListeners( EventType.PRE_UPDATE, listener );
		listenerRegistry.appendListeners( EventType.PRE_DELETE, listener );
		listenerRegistry.appendListeners( EventType.PRE_UPSERT, listener );
		listenerRegistry.appendListeners( EventType.PRE_COLLECTION_UPDATE, listener );
		sessionFactory.addObserver( listener );
	}

	private static boolean isConstraintBasedValidationEnabled(ActivationContext context) {
		if ( context.getServiceRegistry().requireService( ConfigurationService.class )
				.getSetting( APPLY_CONSTRAINTS, StandardConverters.BOOLEAN, true ) ) {
			final var modes = context.getValidationModes();
			return modes.contains( ValidationMode.DDL )
				|| modes.contains( ValidationMode.AUTO );
		}
		else {
			BEAN_VALIDATION_LOGGER.skippingLegacyHVConstraints();
			return false;
		}
	}

	private static void applyRelationalConstraints(ValidatorFactory factory, ActivationContext context) {
		if ( isConstraintBasedValidationEnabled( context ) ) {
			final var serviceRegistry = context.getServiceRegistry();
			applyRelationalConstraints(
					factory,
					context.getMetadata().getEntityBindings(),
					serviceRegistry.requireService( ConfigurationService.class ).getSettings(),
					serviceRegistry.requireService( JdbcServices.class ).getDialect(),
					new ClassLoaderAccessImpl( null,
							serviceRegistry.getService( ClassLoaderService.class ) )
			);
		}
	}

	public static void applyRelationalConstraints(
			ValidatorFactory factory,
			Collection<PersistentClass> persistentClasses,
			Map<String,Object> settings,
			Dialect dialect,
			ClassLoaderAccess classLoaderAccess) {
		final var groups = Set.of( buildGroupsForOperation( Operation.DDL, settings, classLoaderAccess ) );
		final Map<Class<? extends Annotation>, Boolean> constraintCompositionTypeCache = new HashMap<>();
		for ( var persistentClass : persistentClasses ) {
			final String className = persistentClass.getClassName();
			if ( isNotEmpty( className ) ) {
				final var entityClass = entityClass( classLoaderAccess, className );
				try {
					applyDDL(
							"",
							persistentClass,
							entityClass,
							factory,
							groups,
							true,
							dialect,
							constraintCompositionTypeCache
					);
				}
				catch (Exception e) {
					BEAN_VALIDATION_LOGGER.unableToApplyConstraints( className, e );
				}
			}
		}
	}

	private static Class<?> entityClass(ClassLoaderAccess classLoaderAccess, String className) {
		try {
			return classLoaderAccess.classForName( className );
		}
		catch (ClassLoadingException e) {
			throw new AssertionFailure( "Entity class not found", e );
		}
	}

	private static void applyDDL(
			String prefix,
			PersistentClass persistentClass,
			Class<?> clazz,
			ValidatorFactory factory,
			Set<Class<?>> groups,
			boolean activateNotNull,
			Dialect dialect,
			Map<Class<? extends Annotation>, Boolean> constraintCompositionTypeCache
	) {
		final var beanDescriptor = factory.getValidator().getConstraintsForClass( clazz );
		//cno bean level constraints can be applied, go to the properties
		for ( var propertyDescriptor : beanDescriptor.getConstrainedProperties() ) {
			final var property =
					findPropertyByName( persistentClass,
							prefix + propertyDescriptor.getPropertyName() );
			if ( property != null ) {
				final var bounds = new Bounds();
				final boolean hasNotNull = applyConstraints(
						propertyDescriptor.getConstraintDescriptors(),
						property,
						propertyDescriptor,
						groups,
						activateNotNull,
						false,
						dialect,
						constraintCompositionTypeCache,
						bounds
				);
				bounds.apply( property, dialect );
				if ( property.isComposite() && propertyDescriptor.isCascaded() ) {
					final var component = (Component) property.getValue();
					applyDDL(
							prefix + propertyDescriptor.getPropertyName() + ".",
							persistentClass,
							component.getComponentClass(),
							factory,
							groups,
							// we can apply not null if the upper component lets us
							// activate not null and if the property is not null.
							// Otherwise, all sub columns should be left nullable
							activateNotNull && hasNotNull,
							dialect,
							constraintCompositionTypeCache
					);
				}
			}
		}
	}

	private static boolean applyConstraints(
			Set<ConstraintDescriptor<?>> constraintDescriptors,
			Property property,
			PropertyDescriptor propertyDesc,
			Set<Class<?>> groups,
			boolean canApplyNotNull,
			boolean useOrLogicForComposedConstraint,
			Dialect dialect,
			Map<Class<? extends Annotation>, Boolean> constraintCompositionTypeCache,
			Bounds bounds) {

		boolean firstItem = true;
		boolean composedResultHasNotNull = false;
		for ( var constraintDescriptor : constraintDescriptors ) {
			boolean hasNotNull = false;

			if ( groups == null || !disjoint( constraintDescriptor.getGroups(), groups ) ) {
				if ( canApplyNotNull ) {
					hasNotNull = isNotNullDescriptor( constraintDescriptor );
				}

				// apply bean validation specific constraints
				applyDigits( property, constraintDescriptor );
				applySize( property, constraintDescriptor, propertyDesc );

				// collect range bounds - only the most restrictive will be applied
				bounds.lower( extractLowerBound( constraintDescriptor ) );
				bounds.upper( extractUpperBound( constraintDescriptor ) );

				// Apply Hibernate Validator specific constraints - we cannot import any HV specific classes though!
				// No need to check explicitly for @Range. @Range is a composed constraint using @Min and @Max which
				// will be taken care of later.
				applyLength( property, constraintDescriptor, propertyDesc );

				// Composing constraints
				if ( !constraintDescriptor.getComposingConstraints().isEmpty() ) {
					final boolean isOrComposition =
							isConstraintCompositionOfTypeOr( constraintDescriptor, constraintCompositionTypeCache );
					// OR composition uses a separate Bounds with least-restrictive merging;
					// AND composition shares the parent's Bounds
					final var composingBounds = isOrComposition ? new Bounds( true ) : bounds;
					final boolean hasNotNullFromComposingConstraints = applyConstraints(
							constraintDescriptor.getComposingConstraints(),
							property, propertyDesc, null,
							canApplyNotNull,
							isOrComposition,
							dialect,
							constraintCompositionTypeCache,
							composingBounds
					);
					if ( isOrComposition ) {
						composingBounds.mergeInto( bounds, property, dialect );
					}
					hasNotNull |= hasNotNullFromComposingConstraints;
				}
			}

			if ( firstItem ) {
				composedResultHasNotNull = hasNotNull;
				firstItem = false;
			}
			else if ( !useOrLogicForComposedConstraint ) {
				// If the constraint composition is of type AND (default) then only ONE constraint needs to
				// be non-nullable for the property to be marked as 'not-null'.
				composedResultHasNotNull |= hasNotNull;
			}
			else {
				// If the constraint composition is of type OR then ALL constraints need to
				// be non-nullable for the property to be marked as 'not-null'.
				composedResultHasNotNull &= hasNotNull;
			}
		}

		if ( composedResultHasNotNull ) {
			markNotNull( property );
		}

		return composedResultHasNotNull;
	}

	private static boolean isConstraintCompositionOfTypeOr(
			ConstraintDescriptor<?> descriptor,
			Map<Class<? extends Annotation>, Boolean> constraintCompositionTypeCache) {
		if ( descriptor.getComposingConstraints().size() < 2 ) {
			return false;
		}
		else {
			final var composedAnnotation = descriptor.getAnnotation().annotationType();
			return constraintCompositionTypeCache.computeIfAbsent( composedAnnotation, value -> {
				for ( var annotation : value.getAnnotations() ) {
					if ( "org.hibernate.validator.constraints.ConstraintComposition"
							.equals( annotation.annotationType().getName() ) ) {
						try {
							final Method valueMethod = annotation.annotationType().getMethod( "value" );
							final Object result = valueMethod.invoke( annotation );
							return result != null && "OR".equals( result.toString() );
					}
					catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
						BEAN_VALIDATION_LOGGER.constraintCompositionTypeUnknown( ex );
						return false;
					}
					}
				}
				return false;
			} );
		}
	}

	private record Bound(BigDecimal value, boolean inclusive) {
		String lowerCondition() {
			return (inclusive ? ">=" : ">") + value.toPlainString();
		}

		String upperCondition() {
			return (inclusive ? "<=" : "<") + value.toPlainString();
		}
	}

	private static final class Bounds {
		private Bound lower;
		private Bound upper;
		private final boolean orComposition;

		Bounds() {
			this( false );
		}

		Bounds(boolean orComposition) {
			this.orComposition = orComposition;
		}

		public void lower(Bound bound) {
			lower = orComposition
					? leastRestrictiveLowerBound( lower, bound )
					: moreRestrictiveLowerBound( lower, bound );
		}

		public void upper(Bound bound) {
			upper = orComposition
					? leastRestrictiveUpperBound( upper, bound )
					: moreRestrictiveUpperBound( upper, bound );
		}

		public void apply(Property property, Dialect dialect) {
			if ( lower == null && upper == null ) {
				return;
			}
			if ( orComposition && lower != null && upper != null ) {
				final String lowerCondition = lower.lowerCondition();
				final String upperCondition = upper.upperCondition();

				for ( var selectable : property.getSelectables() ) {
					if ( selectable instanceof Column column ) {
						final String name = column.getQuotedName( dialect );
						applySQLCheck( column, name + lowerCondition + " OR " + name + upperCondition );
					}
				}
			}
			else {
				if ( lower != null ) {
					applyBound( property, lower.lowerCondition(), dialect );
				}
				if ( upper != null ) {
					applyBound( property, upper.upperCondition(), dialect );
				}
			}
		}

		private static void applyBound(Property property, String condition, Dialect dialect) {
			for ( var selectable : property.getSelectables() ) {
				if ( selectable instanceof Column column ) {
					applySQLCheck( column, column.getQuotedName( dialect ) + condition);
				}
			}
		}
		private static void applySQLCheck(Column column, String checkConstraint) {
			// need to check whether the new check is already part of the existing check,
			// because applyDDL can be called multiple times
			for ( var columnCheckConstraint : column.getCheckConstraints() ) {
				if ( columnCheckConstraint.getConstraint().equalsIgnoreCase( checkConstraint ) ) {
					return; //EARLY EXIT
				}
			}
			column.addCheckConstraint( new CheckConstraint( checkConstraint ) );
		}

		public void mergeInto(Bounds parent, Property property, Dialect dialect) {
			if ( lower != null && upper != null ) {
				// both sides present - can't reduce to a single bound, apply an OR expression
				apply( property, dialect );
			}
			else {
				// single side - merge into parent so it participates in most-restrictive selection
				parent.lower( lower );
				parent.upper( upper );
			}
		}

		public String toString() {
			return "Bounds[" +
				"lower=" + lower + ", " +
				"upper=" + upper +
				", orComposition=" + orComposition + ']';
		}
	}

	@SuppressWarnings("unchecked")
	private static Bound extractLowerBound(ConstraintDescriptor<?> descriptor) {
		final var annotationType = descriptor.getAnnotation().annotationType();
		if ( Min.class.equals( annotationType ) ) {
			return new Bound( BigDecimal.valueOf( ( (ConstraintDescriptor<Min>) descriptor ).getAnnotation().value() ), true );
		}
		else if ( DecimalMin.class.equals( annotationType ) ) {
			final var annotation = ( (ConstraintDescriptor<DecimalMin>) descriptor ).getAnnotation();
			return new Bound( new BigDecimal( annotation.value() ), annotation.inclusive() );
		}
		else if ( Positive.class.equals( annotationType ) ) {
			return new Bound( BigDecimal.ZERO, false );
		}
		else if ( PositiveOrZero.class.equals( annotationType ) ) {
			return new Bound( BigDecimal.ZERO, true );
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static Bound extractUpperBound(ConstraintDescriptor<?> descriptor) {
		final var annotationType = descriptor.getAnnotation().annotationType();
		if ( Max.class.equals( annotationType ) ) {
			return new Bound( BigDecimal.valueOf( ( (ConstraintDescriptor<Max>) descriptor ).getAnnotation().value() ), true );
		}
		else if ( DecimalMax.class.equals( annotationType ) ) {
			final var annotation = ( (ConstraintDescriptor<DecimalMax>) descriptor ).getAnnotation();
			return new Bound( new BigDecimal( annotation.value() ), annotation.inclusive() );
		}
		else if ( Negative.class.equals( annotationType ) ) {
			return new Bound( BigDecimal.ZERO, false );
		}
		else if ( NegativeOrZero.class.equals( annotationType ) ) {
			return new Bound( BigDecimal.ZERO, true );
		}
		return null;
	}

	private static Bound moreRestrictiveLowerBound(Bound a, Bound b) {
		return selectBound( a, b, true, true );
	}

	private static Bound moreRestrictiveUpperBound(Bound a, Bound b) {
		return selectBound( a, b, false, true );
	}

	private static Bound leastRestrictiveLowerBound(Bound a, Bound b) {
		return selectBound( a, b, false, false );
	}

	private static Bound leastRestrictiveUpperBound(Bound a, Bound b) {
		return selectBound( a, b, true, false );
	}

	private static Bound selectBound(Bound a, Bound b, boolean preferHigherValue, boolean preferExclusive) {
		if ( a == null ) {
			return b;
		}
		if ( b == null ) {
			return a;
		}
		final int cmp = a.value.compareTo( b.value );
		if ( cmp != 0 ) {
			return ( cmp > 0 ) == preferHigherValue ? a : b;
		}
		return ( !a.inclusive ) == preferExclusive ? a : b;
	}

	private static boolean isNotNullDescriptor(ConstraintDescriptor<?> descriptor) {
		final var annotationType = descriptor.getAnnotation().annotationType();
		return NotNull.class.equals(annotationType)
			|| NotEmpty.class.equals(annotationType)
			|| NotBlank.class.equals(annotationType);
	}

	private static void markNotNull(Property property) {
		// single table inheritance should not be forced to null due to shared state
		if ( !( property.getPersistentClass() instanceof SingleTableSubclass )
				// composite should not add not-null on all columns
				&& !property.isComposite() ) {
			property.setOptional( false );
			for ( var selectable : property.getSelectables() ) {
				if ( selectable instanceof Column column ) {
					column.setNullable( false );
				}
				else {
					BEAN_VALIDATION_LOGGER.notNullOnFormulaPortion( property.getName() );
				}
			}
		}
		property.setOptional( false );
	}

	private static void applyDigits(Property property, ConstraintDescriptor<?> descriptor) {
		if ( Digits.class.equals( descriptor.getAnnotation().annotationType() ) ) {
			@SuppressWarnings("unchecked")
			final var digitsConstraint = (ConstraintDescriptor<Digits>) descriptor;
			final int integerDigits = digitsConstraint.getAnnotation().integer();
			final int fractionalDigits = digitsConstraint.getAnnotation().fraction();
			for ( var selectable : property.getSelectables() ) {
				if ( selectable instanceof Column column ) {
					column.setPrecision( integerDigits + fractionalDigits );
					column.setScale( fractionalDigits );
				}
			}

		}
	}

	private static void applySize(Property property, ConstraintDescriptor<?> descriptor, PropertyDescriptor propertyDescriptor) {
		if ( Size.class.equals( descriptor.getAnnotation().annotationType() )
				&& String.class.equals( propertyDescriptor.getElementClass() ) ) {
			@SuppressWarnings("unchecked")
			final var sizeConstraint = (ConstraintDescriptor<Size>) descriptor;
			final int max = sizeConstraint.getAnnotation().max();
			for ( var column : property.getColumns() ) {
				if ( max < Integer.MAX_VALUE ) {
					column.setLength( max );
				}
			}
		}
	}

	private static void applyLength(Property property, ConstraintDescriptor<?> descriptor, PropertyDescriptor propertyDescriptor) {
		if ( isValidatorLengthAnnotation( descriptor )
				&& String.class.equals( propertyDescriptor.getElementClass() ) ) {
			final int max = (Integer) descriptor.getAttributes().get( "max" );
			for ( var selectable : property.getSelectables() ) {
				if ( selectable instanceof Column column ) {
					if ( max < Integer.MAX_VALUE ) {
						column.setLength( max );
					}
				}
			}
		}
	}

	private static boolean isValidatorLengthAnnotation(ConstraintDescriptor<?> descriptor) {
		return "org.hibernate.validator.constraints.Length"
				.equals( descriptor.getAnnotation().annotationType().getName() );
	}

	private static ValidatorFactory getValidatorFactory(ActivationContext context) {
		// IMPL NOTE: We can either be provided a ValidatorFactory or make one. We can be provided
		//            a ValidatorFactory in 2 different ways. So here we "get" a ValidatorFactory
		//            in the following order:
		//		1) Look into SessionFactoryOptions.getValidatorFactoryReference()
		//		2) Look into ConfigurationService
		//		3) build a new ValidatorFactory

		// 1 - look in SessionFactoryOptions.getValidatorFactoryReference()
		final var providedFactory =
				resolveProvidedFactory( context.getSessionFactory().getSessionFactoryOptions() );
		if ( providedFactory != null ) {
			return providedFactory;
		}

		// 2 - look in ConfigurationService
		final var configuredFactory =
				resolveProvidedFactory( context.getServiceRegistry().requireService( ConfigurationService.class ) );
		if ( configuredFactory != null ) {
			return configuredFactory;
		}

		// 3 - build our own
		try {
			return Validation.buildDefaultValidatorFactory();
		}
		catch ( Exception e ) {
			throw new IntegrationException( "Unable to build the default ValidatorFactory", e );
		}
	}

	private static ValidatorFactory resolveProvidedFactory(SessionFactoryOptions options) {
		final Object validatorFactoryReference = options.getValidatorFactoryReference();
		if ( validatorFactoryReference == null ) {
			return null;
		}
		else if ( validatorFactoryReference instanceof ValidatorFactory result ) {
			return result;
		}
		else {
			throw new IntegrationException(
					String.format(
							Locale.ENGLISH,
							"ValidatorFactory reference (provided via %s) was not castable to %s : %s",
							SessionFactoryOptions.class.getName(),
							ValidatorFactory.class.getName(),
							validatorFactoryReference.getClass().getName()
					)
			);
		}
	}

	private static ValidatorFactory resolveProvidedFactory(ConfigurationService cfgService) {
		return cfgService.getSetting(
				JPA_VALIDATION_FACTORY,
				value -> validatorFactory( value, JPA_VALIDATION_FACTORY ),
				cfgService.getSetting(
						JAKARTA_VALIDATION_FACTORY,
						value -> validatorFactory( value, JAKARTA_VALIDATION_FACTORY ),
						null
				)
		);
	}

	private static ValidatorFactory validatorFactory(Object value, String setting) {
		if ( value instanceof ValidatorFactory validatorFactory ) {
			return validatorFactory;
		}
		else {
			throw new IntegrationException(
					String.format(
							Locale.ENGLISH,
							"ValidatorFactory reference (provided via '%s' setting) was not an instance of '%s': %s",
							setting,
							ValidatorFactory.class.getName(),
							value.getClass().getName()
					)
			);
		}
	}
}
