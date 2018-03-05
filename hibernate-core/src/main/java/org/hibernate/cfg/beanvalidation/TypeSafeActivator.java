/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.beanvalidation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
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
import org.hibernate.boot.internal.ClassLoaderAccessImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.SingleTableSubclass;

import org.jboss.logging.Logger;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
class TypeSafeActivator {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, TypeSafeActivator.class.getName());

	private static final String FACTORY_PROPERTY = "javax.persistence.validation.factory";

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

		final ConfigurationService cfgService = activationContext.getServiceRegistry().getService( ConfigurationService.class );
		final ClassLoaderService classLoaderService = activationContext.getServiceRegistry().getService( ClassLoaderService.class );

		// de-activate not-null tracking at the core level when Bean Validation is present unless the user explicitly
		// asks for it
		if ( cfgService.getSettings().get( Environment.CHECK_NULLABILITY ) == null ) {
			activationContext.getSessionFactory().getSessionFactoryOptions().setCheckNullability( false );
		}

		final BeanValidationEventListener listener = new BeanValidationEventListener(
				validatorFactory,
				cfgService.getSettings(),
				classLoaderService
		);

		final EventListenerRegistry listenerRegistry = activationContext.getServiceRegistry()
				.getService( EventListenerRegistry.class );

		listenerRegistry.addDuplicationStrategy( DuplicationStrategyImpl.INSTANCE );

		listenerRegistry.appendListeners( EventType.PRE_INSERT, listener );
		listenerRegistry.appendListeners( EventType.PRE_UPDATE, listener );
		listenerRegistry.appendListeners( EventType.PRE_DELETE, listener );

		listener.initialize( cfgService.getSettings(), classLoaderService );
	}

	@SuppressWarnings({"unchecked", "UnusedParameters"})
	private static void applyRelationalConstraints(ValidatorFactory factory, ActivationContext activationContext) {
		final ConfigurationService cfgService = activationContext.getServiceRegistry().getService( ConfigurationService.class );
		if ( !cfgService.getSetting( BeanValidationIntegrator.APPLY_CONSTRAINTS, StandardConverters.BOOLEAN, true  ) ) {
			LOG.debug( "Skipping application of relational constraints from legacy Hibernate Validator" );
			return;
		}

		final Set<ValidationMode> modes = activationContext.getValidationModes();
		if ( ! ( modes.contains( ValidationMode.DDL ) || modes.contains( ValidationMode.AUTO ) ) ) {
			return;
		}

		applyRelationalConstraints(
				factory,
				activationContext.getMetadata().getEntityBindings(),
				cfgService.getSettings(),
				activationContext.getServiceRegistry().getService( JdbcServices.class ).getDialect(),
				new ClassLoaderAccessImpl(
						null,
						activationContext.getServiceRegistry().getService( ClassLoaderService.class )
				)
		);
	}

	@SuppressWarnings( {"UnusedDeclaration"})
	public static void applyRelationalConstraints(
			ValidatorFactory factory,
			Collection<PersistentClass> persistentClasses,
			Map settings,
			Dialect dialect,
			ClassLoaderAccess classLoaderAccess) {
		Class<?>[] groupsArray = GroupsPerOperation.buildGroupsForOperation(
				GroupsPerOperation.Operation.DDL,
				settings,
				classLoaderAccess
		);
		Set<Class<?>> groups = new HashSet<Class<?>>( Arrays.asList( groupsArray ) );

		for ( PersistentClass persistentClass : persistentClasses ) {
			final String className = persistentClass.getClassName();

			if ( className == null || className.length() == 0 ) {
				continue;
			}
			Class<?> clazz;
			try {
				clazz = classLoaderAccess.classForName( className );
			}
			catch ( ClassLoadingException e ) {
				throw new AssertionFailure( "Entity class not found", e );
			}

			try {
				applyDDL( "", persistentClass, clazz, factory, groups, true, dialect );
			}
			catch (Exception e) {
				LOG.unableToApplyConstraints( className, e );
			}
		}
	}

	private static void applyDDL(
			String prefix,
			PersistentClass persistentClass,
			Class<?> clazz,
			ValidatorFactory factory,
			Set<Class<?>> groups,
			boolean activateNotNull,
			Dialect dialect) {
		final BeanDescriptor descriptor = factory.getValidator().getConstraintsForClass( clazz );
		//no bean level constraints can be applied, go to the properties

		for ( PropertyDescriptor propertyDesc : descriptor.getConstrainedProperties() ) {
			Property property = findPropertyByName( persistentClass, prefix + propertyDesc.getPropertyName() );
			boolean hasNotNull;
			if ( property != null ) {
				hasNotNull = applyConstraints(
						propertyDesc.getConstraintDescriptors(), property, propertyDesc, groups, activateNotNull, dialect
				);
				if ( property.isComposite() && propertyDesc.isCascaded() ) {
					Class<?> componentClass = ( (Component) property.getValue() ).getComponentClass();

					/*
					 * we can apply not null if the upper component let's us activate not null
					 * and if the property is not null.
					 * Otherwise, all sub columns should be left nullable
					 */
					final boolean canSetNotNullOnColumns = activateNotNull && hasNotNull;
					applyDDL(
							prefix + propertyDesc.getPropertyName() + ".",
							persistentClass, componentClass, factory, groups,
							canSetNotNullOnColumns,
                            dialect
					);
				}
				//FIXME add collection of components
			}
		}
	}

	private static boolean applyConstraints(
			Set<ConstraintDescriptor<?>> constraintDescriptors,
			Property property,
			PropertyDescriptor propertyDesc,
			Set<Class<?>> groups,
			boolean canApplyNotNull,
			Dialect dialect) {
		boolean hasNotNull = false;
		for ( ConstraintDescriptor<?> descriptor : constraintDescriptors ) {
			if ( groups != null && Collections.disjoint( descriptor.getGroups(), groups ) ) {
				continue;
			}

			if ( canApplyNotNull ) {
				hasNotNull = hasNotNull || applyNotNull( property, descriptor );
			}

			// apply bean validation specific constraints
			applyDigits( property, descriptor );
			applySize( property, descriptor, propertyDesc );
			applyMin( property, descriptor, dialect );
			applyMax( property, descriptor, dialect );

			// apply hibernate validator specific constraints - we cannot import any HV specific classes though!
			// no need to check explicitly for @Range. @Range is a composed constraint using @Min and @Max which
			// will be taken care later
			applyLength( property, descriptor, propertyDesc );

			// pass an empty set as composing constraints inherit the main constraint and thus are matching already
			boolean hasNotNullFromComposingConstraints = applyConstraints(
					descriptor.getComposingConstraints(),
					property, propertyDesc, null,
					canApplyNotNull,
					dialect
			);

			hasNotNull = hasNotNull || hasNotNullFromComposingConstraints;
		}
		return hasNotNull;
	}

	private static void applyMin(Property property, ConstraintDescriptor<?> descriptor, Dialect dialect) {
		if ( Min.class.equals( descriptor.getAnnotation().annotationType() ) ) {
			@SuppressWarnings("unchecked")
			ConstraintDescriptor<Min> minConstraint = (ConstraintDescriptor<Min>) descriptor;
			long min = minConstraint.getAnnotation().value();

			@SuppressWarnings("unchecked")
			final Iterator<Selectable> itor = property.getColumnIterator();
			if ( itor.hasNext() ) {
				final Selectable selectable = itor.next();
				if ( Column.class.isInstance( selectable ) ) {
					Column col = (Column) selectable;
					String checkConstraint = col.getQuotedName(dialect) + ">=" + min;
					applySQLCheck( col, checkConstraint );
				}
			}
		}
	}

	private static void applyMax(Property property, ConstraintDescriptor<?> descriptor, Dialect dialect) {
		if ( Max.class.equals( descriptor.getAnnotation().annotationType() ) ) {
			@SuppressWarnings("unchecked")
			ConstraintDescriptor<Max> maxConstraint = (ConstraintDescriptor<Max>) descriptor;
			long max = maxConstraint.getAnnotation().value();

			@SuppressWarnings("unchecked")
			final Iterator<Selectable> itor = property.getColumnIterator();
			if ( itor.hasNext() ) {
				final Selectable selectable = itor.next();
				if ( Column.class.isInstance( selectable ) ) {
					Column col = (Column) selectable;
					String checkConstraint = col.getQuotedName( dialect ) + "<=" + max;
					applySQLCheck( col, checkConstraint );
				}
			}
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

	@SuppressWarnings("unchecked")
	private static boolean applyNotNull(Property property, ConstraintDescriptor<?> descriptor) {
		boolean hasNotNull = false;
		if ( NotNull.class.equals( descriptor.getAnnotation().annotationType() ) ) {
			// single table inheritance should not be forced to null due to shared state
			if ( !( property.getPersistentClass() instanceof SingleTableSubclass ) ) {
				//composite should not add not-null on all columns
				if ( !property.isComposite() ) {
					final Iterator<Selectable> itr = property.getColumnIterator();
					while ( itr.hasNext() ) {
						final Selectable selectable = itr.next();
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

			@SuppressWarnings("unchecked")
			final Iterator<Selectable> itor = property.getColumnIterator();
			if ( itor.hasNext() ) {
				final Selectable selectable = itor.next();
				if ( Column.class.isInstance( selectable ) ) {
					Column col = (Column) selectable;
					col.setPrecision( integerDigits + fractionalDigits );
					col.setScale( fractionalDigits );
				}
			}

		}
	}

	private static void applySize(Property property, ConstraintDescriptor<?> descriptor, PropertyDescriptor propertyDescriptor) {
		if ( Size.class.equals( descriptor.getAnnotation().annotationType() )
				&& String.class.equals( propertyDescriptor.getElementClass() ) ) {
			@SuppressWarnings("unchecked")
			ConstraintDescriptor<Size> sizeConstraint = (ConstraintDescriptor<Size>) descriptor;
			int max = sizeConstraint.getAnnotation().max();

			@SuppressWarnings("unchecked")
			final Iterator<Selectable> itor = property.getColumnIterator();
			if ( itor.hasNext() ) {
				final Selectable selectable = itor.next();
				Column col = (Column) selectable;
				if ( max < Integer.MAX_VALUE ) {
					col.setLength( max );
				}
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

			@SuppressWarnings("unchecked")
			final Iterator<Selectable> itor = property.getColumnIterator();
			if ( itor.hasNext() ) {
				final Selectable selectable = itor.next();
				if ( Column.class.isInstance( selectable ) ) {
					Column col = (Column) selectable;
					if ( max < Integer.MAX_VALUE ) {
						col.setLength( max );
					}
				}
			}
		}
	}

	/**
	 * Locate the property by path in a recursive way, including IdentifierProperty in the loop if propertyName is
	 * {@code null}.  If propertyName is {@code null} or empty, the IdentifierProperty is returned
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
						if ( !property.isComposite() ) {
							return null;
						}
						property = ( (Component) property.getValue() ).getProperty( element );
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
					String element = (String) st.nextElement();
					if ( property == null ) {
						property = associatedClass.getIdentifierMapper().getProperty( element );
					}
					else {
						if ( !property.isComposite() ) {
							return null;
						}
						property = ( (Component) property.getValue() ).getProperty( element );
					}
				}
			}
			catch ( MappingException ee ) {
				return null;
			}
		}
		return property;
	}

	private static ValidatorFactory getValidatorFactory(ActivationContext activationContext) {
		// IMPL NOTE : We can either be provided a ValidatorFactory or make one.  We can be provided
		// a ValidatorFactory in 2 different ways.  So here we "get" a ValidatorFactory in the following order:
		//		1) Look into SessionFactoryOptions.getValidatorFactoryReference()
		//		2) Look into ConfigurationService
		//		3) build a new ValidatorFactory

		// 1 - look in SessionFactoryOptions.getValidatorFactoryReference()
		ValidatorFactory factory = resolveProvidedFactory( activationContext.getSessionFactory().getSessionFactoryOptions() );
		if ( factory != null ) {
			return factory;
		}

		// 2 - look in ConfigurationService
		factory = resolveProvidedFactory( activationContext.getServiceRegistry().getService( ConfigurationService.class ) );
		if ( factory != null ) {
			return factory;
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

		try {
			return ValidatorFactory.class.cast( validatorFactoryReference );
		}
		catch ( ClassCastException e ) {
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

	@SuppressWarnings("unchecked")
	private static ValidatorFactory resolveProvidedFactory(ConfigurationService cfgService) {
		return cfgService.getSetting(
				FACTORY_PROPERTY,
				new ConfigurationService.Converter<ValidatorFactory>() {
					@Override
					public ValidatorFactory convert(Object value) {
						try {
							return ValidatorFactory.class.cast( value );
						}
						catch ( ClassCastException e ) {
							throw new IntegrationException(
									String.format(
											Locale.ENGLISH,
											"ValidatorFactory reference (provided via `%s` setting) was not castable to %s : %s",
											FACTORY_PROPERTY,
											ValidatorFactory.class.getName(),
											value.getClass().getName()
									)
							);
						}
					}
				},
				null
		);
	}
}
