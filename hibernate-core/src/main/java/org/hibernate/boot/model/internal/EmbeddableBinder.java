/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.annotations.Instantiator;
import org.hibernate.annotations.TypeBinderType;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.annotations.common.reflection.XMethod;
import org.hibernate.annotations.common.reflection.XProperty;
import org.hibernate.binder.TypeBinder;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.property.access.internal.PropertyAccessStrategyCompositeUserTypeImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyMixedImpl;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.usertype.CompositeUserType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import static org.hibernate.boot.model.internal.AnnotatedDiscriminatorColumn.DEFAULT_DISCRIMINATOR_COLUMN_NAME;
import static org.hibernate.boot.model.internal.AnnotatedDiscriminatorColumn.buildDiscriminatorColumn;
import static org.hibernate.boot.model.internal.BinderHelper.getOverridableAnnotation;
import static org.hibernate.boot.model.internal.BinderHelper.getPath;
import static org.hibernate.boot.model.internal.BinderHelper.getPropertyOverriddenByMapperOrMapsId;
import static org.hibernate.boot.model.internal.BinderHelper.getRelativePath;
import static org.hibernate.boot.model.internal.BinderHelper.hasToOneAnnotation;
import static org.hibernate.boot.model.internal.HCANNHelper.findContainingAnnotations;
import static org.hibernate.boot.model.internal.PropertyBinder.addElementsOfClass;
import static org.hibernate.boot.model.internal.PropertyBinder.processElementAnnotations;
import static org.hibernate.boot.model.internal.PropertyBinder.processId;
import static org.hibernate.boot.model.internal.PropertyHolderBuilder.buildPropertyHolder;
import static org.hibernate.internal.CoreLogging.messageLogger;
import static org.hibernate.internal.util.StringHelper.isEmpty;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.internal.util.StringHelper.unqualify;

/**
 * A binder responsible for interpreting {@link Embeddable} classes and producing
 * instances of the mapping model object {@link Component}.
 */
public class EmbeddableBinder {
	private static final CoreMessageLogger LOG = messageLogger( EmbeddableBinder.class );

	static PropertyBinder createCompositeBinder(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			boolean isComponentEmbedded,
			MetadataBuildingContext context,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			XProperty property,
			AnnotatedColumns columns,
			XClass returnedClass,
			PropertyBinder propertyBinder,
			boolean isOverridden,
			Class<? extends CompositeUserType<?>> compositeUserType) {
		final String referencedEntityName;
		final String propertyName;
		final AnnotatedJoinColumns actualColumns;
		if ( isOverridden ) {
			// careful: not always a @MapsId property, sometimes it's from an @IdClass
			final PropertyData mapsIdProperty = getPropertyOverriddenByMapperOrMapsId(
					propertyBinder.isId(),
					propertyHolder,
					property.getName(),
					context
			);
			referencedEntityName = mapsIdProperty.getClassOrElementName();
			propertyName = mapsIdProperty.getPropertyName();
			final AnnotatedJoinColumns parent = new AnnotatedJoinColumns();
			parent.setBuildingContext( context );
			parent.setPropertyHolder( propertyHolder );
			parent.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
			//TODO: resetting the parent here looks like a dangerous thing to do
			//      should we be cloning them first (the legacy code did not)
			for ( AnnotatedColumn column : columns.getColumns() ) {
				column.setParent( parent );
			}
			actualColumns = parent;
		}
		else {
			referencedEntityName = null;
			propertyName = null;
			actualColumns = null;
		}

		return createEmbeddedProperty(
				inferredData,
				propertyHolder,
				entityBinder,
				context,
				isComponentEmbedded,
				propertyBinder.isId(),
				inheritanceStatePerClass,
				bindEmbeddable(
						inferredData,
						propertyHolder,
						entityBinder.getPropertyAccessor( property ),
						entityBinder,
						isIdentifierMapper,
						context,
						isComponentEmbedded,
						propertyBinder.isId(),
						inheritanceStatePerClass,
						referencedEntityName,
						propertyName,
						determineCustomInstantiator( property, returnedClass, context ),
						compositeUserType,
						actualColumns,
						columns
				)
		);
	}

	static boolean isEmbedded(XProperty property, XClass returnedClass) {
		return property.isAnnotationPresent( Embedded.class )
			|| property.isAnnotationPresent( EmbeddedId.class )
			|| returnedClass.isAnnotationPresent( Embeddable.class ) && !property.isAnnotationPresent( Convert.class );
	}

	public static Component bindEmbeddable(
			PropertyData inferredData,
			PropertyHolder propertyHolder,
			AccessType propertyAccessor,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			MetadataBuildingContext context,
			boolean isComponentEmbedded,
			boolean isId, //is an identifier
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			String referencedEntityName, //is a component who is overridden by a @MapsId
			String propertyName,
			Class<? extends EmbeddableInstantiator> customInstantiatorImpl,
			Class<? extends CompositeUserType<?>> compositeUserTypeClass,
			AnnotatedJoinColumns columns,
			AnnotatedColumns annotatedColumns) {
		final Component component;
		if ( referencedEntityName != null ) {
			component = createEmbeddable(
					propertyHolder,
					inferredData,
					isComponentEmbedded,
					isIdentifierMapper,
					customInstantiatorImpl,
					context
			);
			context.getMetadataCollector().addSecondPass( new CopyIdentifierComponentSecondPass(
					component,
					referencedEntityName,
					propertyName,
					columns,
					context
			) );
		}
		else {
			component = fillEmbeddable(
					propertyHolder,
					inferredData,
					propertyAccessor,
					!isId,
					entityBinder,
					isComponentEmbedded,
					isIdentifierMapper,
					context.getMetadataCollector().isInSecondPass(),
					customInstantiatorImpl,
					compositeUserTypeClass,
					annotatedColumns,
					context,
					inheritanceStatePerClass
			);
		}
		if ( isId ) {
			component.setKey( true );
			checkEmbeddedId( inferredData, propertyHolder, referencedEntityName, component );
		}
		callTypeBinders( component, context, inferredData.getPropertyClass() );
		return component;
	}

	private static void callTypeBinders(Component component, MetadataBuildingContext context, XClass annotatedClass ) {
		for ( Annotation containingAnnotation : findContainingAnnotations( annotatedClass, TypeBinderType.class) ) {
			final TypeBinderType binderType = containingAnnotation.annotationType().getAnnotation( TypeBinderType.class );
			try {
				final TypeBinder binder = binderType.binder().newInstance();
				binder.bind( containingAnnotation, context, component );
			}
			catch ( Exception e ) {
				throw new AnnotationException( "error processing @TypeBinderType annotation '" + containingAnnotation + "'", e );
			}
		}
	}

	private static PropertyBinder createEmbeddedProperty(
			PropertyData inferredData,
			PropertyHolder propertyHolder,
			EntityBinder entityBinder,
			MetadataBuildingContext context,
			boolean isComponentEmbedded,
			boolean isId,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			Component component) {
		final PropertyBinder binder = new PropertyBinder();
		binder.setDeclaringClass( inferredData.getDeclaringClass() );
		binder.setName( inferredData.getPropertyName() );
		binder.setValue(component);
		binder.setProperty( inferredData.getProperty() );
		binder.setAccessType( inferredData.getDefaultAccess() );
		binder.setEmbedded(isComponentEmbedded);
		binder.setHolder(propertyHolder);
		binder.setId(isId);
		binder.setEntityBinder(entityBinder);
		binder.setInheritanceStatePerClass(inheritanceStatePerClass);
		binder.setBuildingContext(context);
		binder.makePropertyAndBind();
		return binder;
	}

	private static void checkEmbeddedId(
			PropertyData inferredData,
			PropertyHolder propertyHolder,
			String referencedEntityName,
			Component component) {
		if ( propertyHolder.getPersistentClass().getIdentifier() != null ) {
			throw new AnnotationException(
					"Embeddable class '" + component.getComponentClassName()
							+ "' may not have a property annotated '@Id' since it is used by '"
							+ getPath(propertyHolder, inferredData)
							+ "' as an '@EmbeddedId'"
			);
		}
		if ( referencedEntityName == null && component.getPropertySpan() == 0 ) {
			throw new AnnotationException(
					"Embeddable class '" + component.getComponentClassName()
							+ "' may not be used as an '@EmbeddedId' by '"
							+ getPath(propertyHolder, inferredData)
							+ "' because it has no properties"
			);
		}
	}

	static Component fillEmbeddable(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			AccessType propertyAccessor,
			boolean isNullable,
			EntityBinder entityBinder,
			boolean isComponentEmbedded,
			boolean isIdentifierMapper,
			boolean inSecondPass,
			Class<? extends EmbeddableInstantiator> customInstantiatorImpl,
			Class<? extends CompositeUserType<?>> compositeUserTypeClass,
			AnnotatedColumns columns,
			MetadataBuildingContext context,
			Map<XClass, InheritanceState> inheritanceStatePerClass) {
		return fillEmbeddable(
				propertyHolder,
				inferredData,
				null,
				propertyAccessor,
				isNullable,
				entityBinder,
				isComponentEmbedded,
				isIdentifierMapper,
				inSecondPass,
				customInstantiatorImpl,
				compositeUserTypeClass,
				columns,
				context,
				inheritanceStatePerClass,
				false
		);
	}

	static Component fillEmbeddable(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			PropertyData baseInferredData, //base inferred data correspond to the entity reproducing inferredData's properties (ie IdClass)
			AccessType propertyAccessor,
			boolean isNullable,
			EntityBinder entityBinder,
			boolean isComponentEmbedded,
			boolean isIdentifierMapper,
			boolean inSecondPass,
			Class<? extends EmbeddableInstantiator> customInstantiatorImpl,
			Class<? extends CompositeUserType<?>> compositeUserTypeClass,
			AnnotatedColumns columns,
			MetadataBuildingContext context,
			Map<XClass, InheritanceState> inheritanceStatePerClass,
			boolean isIdClass) {
		// inSecondPass can only be used to apply right away the second pass of a composite-element
		// Because it's a value type, there is no bidirectional association, hence second pass
		// ordering does not matter
		final Component component = createEmbeddable(
				propertyHolder,
				inferredData,
				isComponentEmbedded,
				isIdentifierMapper,
				customInstantiatorImpl,
				context
		);

		final String subpath = getPath( propertyHolder, inferredData );
		LOG.tracev( "Binding component with path: {0}", subpath );
		final PropertyHolder subholder = buildPropertyHolder(
				component,
				subpath,
				inferredData,
				propertyHolder,
				context,
				inheritanceStatePerClass
		);

		// propertyHolder here is the owner of the component property.
		// Tell it we are about to start the component...
		propertyHolder.startingProperty( inferredData.getProperty() );

		final CompositeUserType<?> compositeUserType;
		final XClass returnedClassOrElement;
		if ( compositeUserTypeClass == null ) {
			compositeUserType = null;
			returnedClassOrElement = inferredData.getClassOrPluralElement();
		}
		else {
			compositeUserType = compositeUserType( compositeUserTypeClass, context );
			component.setTypeName( compositeUserTypeClass.getName() );
			returnedClassOrElement = context.getBootstrapContext().getReflectionManager()
					.toXClass( compositeUserType.embeddable() );
		}
		AggregateComponentBinder.processAggregate(
				component,
				propertyHolder,
				inferredData,
				returnedClassOrElement,
				columns,
				context
		);

		final InheritanceState inheritanceState = inheritanceStatePerClass.get( returnedClassOrElement );
		if ( inheritanceState != null ) {
			inheritanceState.postProcess( component );
			// Main entry point for binding embeddable inheritance
			bindDiscriminator(
					component,
					returnedClassOrElement,
					propertyHolder,
					subholder,
					inferredData,
					inheritanceState,
					context
			);
		}

		final XClass annotatedClass = inferredData.getPropertyClass();
		final Map<String, String> subclassToSuperclass = component.isPolymorphic() ? new HashMap<>() : null;
		final List<PropertyData> classElements = collectClassElements(
				propertyAccessor,
				context,
				returnedClassOrElement,
				annotatedClass,
				isIdClass,
				subclassToSuperclass
		);

		if ( component.isPolymorphic() ) {
			validateInheritanceIsSupported( subholder, compositeUserType );
			final BasicType<?> discriminatorType = (BasicType<?>) component.getDiscriminator().getType();
			// Discriminator values are used to construct the embeddable domain
			// type hierarchy so order of processing is important
			final Map<Object, String> discriminatorValues = new TreeMap<>();
			collectDiscriminatorValue( returnedClassOrElement, discriminatorType, discriminatorValues );
			collectSubclassElements(
					propertyAccessor,
					context,
					returnedClassOrElement,
					classElements,
					discriminatorType,
					discriminatorValues,
					subclassToSuperclass
			);
			component.setDiscriminatorValues( discriminatorValues );
			component.setSubclassToSuperclass( subclassToSuperclass );
		}

		final List<PropertyData> baseClassElements =
				collectBaseClassElements( baseInferredData, propertyAccessor, context, annotatedClass );
		if ( baseClassElements != null
				//useful to avoid breaking pre JPA 2 mappings
				&& !hasAnnotationsOnIdClass( annotatedClass ) ) {
			processIdClassElements( propertyHolder, baseInferredData, classElements, baseClassElements );
		}
		for ( PropertyData propertyAnnotatedElement : classElements ) {
			processElementAnnotations(
					subholder,
					entityBinder.getPersistentClass() instanceof SingleTableSubclass
							// subclasses in single table inheritance can't have not null constraints
							? Nullability.FORCED_NULL
							: ( isNullable ? Nullability.NO_CONSTRAINT : Nullability.FORCED_NOT_NULL ),
					propertyAnnotatedElement,
					new HashMap<>(),
					entityBinder,
					isIdentifierMapper,
					isComponentEmbedded,
					inSecondPass,
					context,
					inheritanceStatePerClass
			);

			final XProperty member = propertyAnnotatedElement.getProperty();
			if ( isIdClass || subholder.isOrWithinEmbeddedId() ) {
				final Property property = findProperty( component, member.getName() );
				if ( property != null ) {
					// Identifier properties are always simple values
					final SimpleValue value = (SimpleValue) property.getValue();
					processId(
							subholder,
							propertyAnnotatedElement,
							value,
							Map.of(),
							context
					);
				}
			}
			else if ( member.isAnnotationPresent( GeneratedValue.class ) ) {
				throw new AnnotationException(
						"Property '" + member.getName() + "' of '"
								+ getPath( propertyHolder, inferredData )
								+ "' is annotated '@GeneratedValue' but is not part of an identifier" );
			}
		}

		if ( compositeUserType != null ) {
			processCompositeUserType( component, compositeUserType );
		}

		return component;
	}

	private static Property findProperty(Component component, String name) {
		for ( Property property : component.getProperties() ) {
			if ( property.getName().equals( name ) ) {
				return property;
			}
		}
		return null;
	}

	private static CompositeUserType<?> compositeUserType(
			Class<? extends CompositeUserType<?>> compositeUserTypeClass,
			MetadataBuildingContext context) {
		if ( !context.getBuildingOptions().isAllowExtensionsInCdi() ) {
			FallbackBeanInstanceProducer.INSTANCE.produceBeanInstance( compositeUserTypeClass );
		}

		return context.getBootstrapContext()
				.getServiceRegistry()
				.requireService( ManagedBeanRegistry.class )
				.getBean( compositeUserTypeClass )
				.getBeanInstance();
	}

	private static void bindDiscriminator(
			Component component,
			XClass componentClass,
			PropertyHolder parentHolder,
			PropertyHolder holder,
			PropertyData propertyData,
			InheritanceState inheritanceState,
			MetadataBuildingContext context) {
		if ( inheritanceState == null ) {
			return;
		}
		final AnnotatedDiscriminatorColumn discriminatorColumn = processEmbeddableDiscriminatorProperties(
				componentClass,
				propertyData,
				parentHolder,
				holder,
				inheritanceState,
				context
		);
		if ( discriminatorColumn != null ) {
			bindDiscriminatorColumnToComponent( component, discriminatorColumn, holder, context );
		}
	}

	private static AnnotatedDiscriminatorColumn processEmbeddableDiscriminatorProperties(
			XClass annotatedClass,
			PropertyData propertyData,
			PropertyHolder parentHolder,
			PropertyHolder holder,
			InheritanceState inheritanceState,
			MetadataBuildingContext context) {
		final DiscriminatorColumn discriminatorColumn = annotatedClass.getAnnotation( DiscriminatorColumn.class );
		final DiscriminatorFormula discriminatorFormula = getOverridableAnnotation(
				annotatedClass,
				DiscriminatorFormula.class,
				context
		);
		if ( !inheritanceState.hasParents() ) {
			if ( inheritanceState.hasSiblings() ) {
				final String path = qualify( holder.getPath(), EntityDiscriminatorMapping.DISCRIMINATOR_ROLE_NAME );
				final String columnPrefix;
				final Column[] overrides;
				if ( holder.isWithinElementCollection() ) {
					columnPrefix = unqualify( parentHolder.getPath() );
					overrides = parentHolder.getOverriddenColumn( path );
				}
				else {
					columnPrefix = propertyData.getPropertyName();
					overrides = holder.getOverriddenColumn( path );
				}
				return buildDiscriminatorColumn(
						discriminatorColumn,
						discriminatorFormula,
						overrides == null ? null : overrides[0],
						columnPrefix + "_" + DEFAULT_DISCRIMINATOR_COLUMN_NAME,
						context
				);
			}
		}
		else {
			// not a root entity
			if ( discriminatorColumn != null ) {
				throw new AnnotationException( String.format(
						"Embeddable class '%s' is annotated '@DiscriminatorColumn' but it is not the root of the inheritance hierarchy",
						annotatedClass.getName()
				) );
			}
			if ( discriminatorFormula != null ) {
				throw new AnnotationException( String.format(
						"Embeddable class '%s' is annotated '@DiscriminatorFormula' but it is not the root of the inheritance hierarchy",
						annotatedClass.getName()
				) );
			}
		}
		return null;
	}

	private static void bindDiscriminatorColumnToComponent(
			Component component,
			AnnotatedDiscriminatorColumn discriminatorColumn,
			PropertyHolder holder,
			MetadataBuildingContext context) {
		assert component.getDiscriminator() == null;
		LOG.tracev( "Setting discriminator for embeddable {0}", component.getComponentClassName() );
		final AnnotatedColumns columns = new AnnotatedColumns();
		columns.setPropertyHolder( holder );
		columns.setBuildingContext( context );
		discriminatorColumn.setParent( columns );
		final BasicValue discriminatorColumnBinding = new BasicValue( context, component.getTable() );
		discriminatorColumnBinding.setAggregateColumn( component.getAggregateColumn() );
		component.setDiscriminator( discriminatorColumnBinding );
		discriminatorColumn.linkWithValue( discriminatorColumnBinding );
		discriminatorColumnBinding.setTypeName( discriminatorColumn.getDiscriminatorTypeName() );
	}

	private static void validateInheritanceIsSupported(
			PropertyHolder holder,
			CompositeUserType<?> compositeUserType) {
		if ( holder.isOrWithinEmbeddedId() ) {
			throw new AnnotationException( String.format(
					"Embeddable class '%s' defines an inheritance hierarchy and cannot be used in an '@EmbeddedId'",
					holder.getClassName()
			) );
		}
		else if ( holder.isInIdClass() ) {
			throw new AnnotationException( String.format(
					"Embeddable class '%s' defines an inheritance hierarchy and cannot be used in an '@IdClass'",
					holder.getClassName()
			) );
		}
		else if ( compositeUserType != null ) {
			throw new AnnotationException( String.format(
					"Embeddable class '%s' defines an inheritance hierarchy and cannot be used with a custom '@CompositeType'",
					holder.getClassName()
			) );
		}
	}

	private static List<PropertyData> collectClassElements(
			AccessType propertyAccessor,
			MetadataBuildingContext context,
			XClass returnedClassOrElement,
			XClass annotatedClass,
			boolean isIdClass,
			Map<String, String> subclassToSuperclass) {
		final List<PropertyData> classElements = new ArrayList<>();
		//embeddable elements can have type defs
		final PropertyContainer container =
				new PropertyContainer( returnedClassOrElement, annotatedClass, propertyAccessor );
		addElementsOfClass( classElements, container, context, 0 );
		//add elements of the embeddable's mapped superclasses
		XClass subclass = returnedClassOrElement;
		XClass superClass;
		while ( isValidSuperclass( superClass = subclass.getSuperclass(), isIdClass ) ) {
			//FIXME: proper support of type variables incl var resolved at upper levels
			final PropertyContainer superContainer =
					new PropertyContainer( superClass, annotatedClass, propertyAccessor );
			addElementsOfClass( classElements, superContainer, context, 0 );
			if ( subclassToSuperclass != null ) {
				subclassToSuperclass.put( subclass.getName(), superClass.getName() );
			}
			subclass = superClass;
		}
		return classElements;
	}

	private static void collectSubclassElements(
			AccessType propertyAccessor,
			MetadataBuildingContext context,
			XClass superclass,
			List<PropertyData> classElements,
			BasicType<?> discriminatorType,
			Map<Object, String> discriminatorValues,
			Map<String, String> subclassToSuperclass) {
		for ( final XClass subclass : context.getMetadataCollector().getEmbeddableSubclasses( superclass ) ) {
			// collect the discriminator value details
			final String old = collectDiscriminatorValue( subclass, discriminatorType, discriminatorValues );
			if ( old != null ) {
				throw new AnnotationException( String.format(
						"Embeddable subclass '%s' defines the same discriminator value as '%s",
						subclass.getName(),
						old
				) );
			}
			final String put = subclassToSuperclass.put( subclass.getName().intern(), superclass.getName().intern() );
			assert put == null;
			// collect property of subclass
			final PropertyContainer superContainer = new PropertyContainer( subclass, superclass, propertyAccessor );
			addElementsOfClass( classElements, superContainer, context, 0 );
			// recursively do that same for all subclasses
			collectSubclassElements(
					propertyAccessor,
					context,
					subclass,
					classElements,
					discriminatorType,
					discriminatorValues,
					subclassToSuperclass
			);
		}
	}

	private static String collectDiscriminatorValue(
			XClass annotatedClass,
			BasicType<?> discriminatorType,
			Map<Object, String> discriminatorValues) {
		final String explicitValue = annotatedClass.isAnnotationPresent( DiscriminatorValue.class )
				? annotatedClass.getAnnotation( DiscriminatorValue.class ).value()
				: null;
		final String discriminatorValue;
		if ( isEmpty( explicitValue ) ) {
			final String name = unqualify( annotatedClass.getName() );
			if ( "character".equals( discriminatorType.getName() ) ) {
				throw new AnnotationException( String.format(
						"Embeddable '%s' has a discriminator of character type and must specify its '@DiscriminatorValue'",
						name
				) );
			}
			else if ( "integer".equals( discriminatorType.getName() ) ) {
				discriminatorValue = String.valueOf( name.hashCode() );
			}
			else {
				discriminatorValue = name;
			}
		}
		else {
			discriminatorValue = explicitValue;
		}
		return discriminatorValues.put(
				discriminatorType.getJavaTypeDescriptor().fromString( discriminatorValue ),
				annotatedClass.getName().intern()
		);
	}


	private static boolean isValidSuperclass(XClass superClass, boolean isIdClass) {
		if ( superClass == null ) {
			return false;
		}

		return superClass.isAnnotationPresent( MappedSuperclass.class )
				|| ( isIdClass
				&& !superClass.getName().equals( Object.class.getName() )
				&& !superClass.getName().equals( "java.lang.Record" ) );
	}

	private static List<PropertyData> collectBaseClassElements(
			PropertyData baseInferredData,
			AccessType propertyAccessor,
			MetadataBuildingContext context,
			XClass annotatedClass) {
		if ( baseInferredData != null ) {
			final List<PropertyData> baseClassElements = new ArrayList<>();
			// iterate from base returned class up hierarchy to handle cases where the @Id attributes
			// might be spread across the subclasses and super classes.
			XClass baseReturnedClassOrElement = baseInferredData.getClassOrElement();
			while ( !Object.class.getName().equals( baseReturnedClassOrElement.getName() ) ) {
				final PropertyContainer container =
						new PropertyContainer( baseReturnedClassOrElement, annotatedClass, propertyAccessor );
				addElementsOfClass( baseClassElements, container, context, 0 );
				baseReturnedClassOrElement = baseReturnedClassOrElement.getSuperclass();
			}
			return baseClassElements;
		}
		else {
			return null;
		}
	}

	private static void processCompositeUserType(Component component, CompositeUserType<?> compositeUserType) {
		component.sortProperties();
		final List<String> sortedPropertyNames = new ArrayList<>( component.getPropertySpan() );
		final List<Type> sortedPropertyTypes = new ArrayList<>( component.getPropertySpan() );
		final PropertyAccessStrategy strategy = new PropertyAccessStrategyCompositeUserTypeImpl(
				compositeUserType,
				sortedPropertyNames,
				sortedPropertyTypes
		);
		for ( Property property : component.getProperties() ) {
			sortedPropertyNames.add( property.getName() );
			sortedPropertyTypes.add(
					PropertyAccessStrategyMixedImpl.INSTANCE.buildPropertyAccess(
							compositeUserType.embeddable(),
							property.getName(),
							false
					).getGetter().getReturnType()
			);
			property.setPropertyAccessStrategy( strategy );
		}
	}

	private static boolean hasAnnotationsOnIdClass(XClass idClass) {
		for ( XProperty property : idClass.getDeclaredProperties( XClass.ACCESS_FIELD ) ) {
			if ( hasTriggeringAnnotation( property ) ) {
				return true;
			}
		}
		for ( XMethod method : idClass.getDeclaredMethods() ) {
			if ( hasTriggeringAnnotation( method ) ) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasTriggeringAnnotation(XAnnotatedElement property) {
		return property.isAnnotationPresent(Column.class)
			|| property.isAnnotationPresent(OneToMany.class)
			|| property.isAnnotationPresent(ManyToOne.class)
			|| property.isAnnotationPresent(Id.class)
			|| property.isAnnotationPresent(GeneratedValue.class)
			|| property.isAnnotationPresent(OneToOne.class)
			|| property.isAnnotationPresent(ManyToMany.class);
	}

	private static void processIdClassElements(
			PropertyHolder propertyHolder,
			PropertyData baseInferredData,
			List<PropertyData> classElements,
			List<PropertyData> baseClassElements) {
		final Map<String, PropertyData> baseClassElementsByName = new HashMap<>();
		for ( PropertyData element : baseClassElements ) {
			baseClassElementsByName.put( element.getPropertyName(), element );
		}

		for ( int i = 0; i < classElements.size(); i++ ) {
			final PropertyData idClassPropertyData = classElements.get( i );
			final PropertyData entityPropertyData =
					baseClassElementsByName.get( idClassPropertyData.getPropertyName() );
			if ( propertyHolder.isInIdClass() ) {
				if ( entityPropertyData == null ) {
					throw new AnnotationException(
							"Property '" + getPath(propertyHolder, idClassPropertyData )
									+ "' belongs to an '@IdClass' but has no matching property in entity class '"
									+ baseInferredData.getPropertyClass().getName()
									+ "' (every property of the '@IdClass' must have a corresponding persistent property in the '@Entity' class)"
					);
				}
				if ( hasToOneAnnotation( entityPropertyData.getProperty() )
						&& !entityPropertyData.getClassOrElement().equals( idClassPropertyData.getClassOrElement() ) ) {
					//don't replace here as we need to use the actual original return type
					//the annotation overriding will be dealt with by a mechanism similar to @MapsId
					continue;
				}
			}
			classElements.set( i, entityPropertyData );  //this works since they are in the same order
		}
	}

	static Component createEmbeddable(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			boolean isComponentEmbedded,
			boolean isIdentifierMapper,
			Class<? extends EmbeddableInstantiator> customInstantiatorImpl,
			MetadataBuildingContext context) {
		final Component component = new Component( context, propertyHolder.getPersistentClass() );
		component.setEmbedded( isComponentEmbedded );
		//yuk
		component.setTable( propertyHolder.getTable() );
		final XClass embeddableClass;
		//FIXME shouldn't identifier mapper use getClassOrElementName? Need to be checked.
		if ( isIdentifierMapper
				|| isComponentEmbedded && inferredData.getPropertyName() == null ) {
			component.setComponentClassName( component.getOwner().getClassName() );
			embeddableClass = inferredData.getClassOrElement();
		}
		else {
			embeddableClass = inferredData.getClassOrPluralElement();
			component.setComponentClassName( embeddableClass.getName() );
		}
		component.setCustomInstantiator( customInstantiatorImpl );
		final Constructor<?> constructor = resolveInstantiator( embeddableClass, context );
		if ( constructor != null ) {
			component.setInstantiator( constructor, constructor.getAnnotation( Instantiator.class ).value() );
		}
		if ( propertyHolder.isComponent() ) {
			final ComponentPropertyHolder componentPropertyHolder = (ComponentPropertyHolder) propertyHolder;
			component.setParentAggregateColumn( componentPropertyHolder.getAggregateColumn() );
		}
		return component;
	}

	private static Constructor<?> resolveInstantiator(XClass embeddableClass, MetadataBuildingContext buildingContext) {
		if ( embeddableClass != null ) {
			final Constructor<?>[] declaredConstructors = buildingContext.getBootstrapContext().getReflectionManager()
					.toClass( embeddableClass )
					.getDeclaredConstructors();
			Constructor<?> constructor = null;
			for ( Constructor<?> declaredConstructor : declaredConstructors ) {
				if ( declaredConstructor.isAnnotationPresent( Instantiator.class ) ) {
					if ( constructor != null ) {
						throw new AnnotationException( "Multiple constructors of '" + embeddableClass.getName()
								+ "' are annotated '@Instantiator' but only one constructor can be the canonical constructor" );
					}
					constructor = declaredConstructor;
				}
			}
			return constructor;
		}
		return null;
	}

	public static Class<? extends EmbeddableInstantiator> determineCustomInstantiator(
			XProperty property,
			XClass returnedClass,
			MetadataBuildingContext context) {
		if ( property.isAnnotationPresent( EmbeddedId.class ) ) {
			// we don't allow custom instantiators for composite ids
			return null;
		}

		final org.hibernate.annotations.EmbeddableInstantiator propertyAnnotation =
				property.getAnnotation( org.hibernate.annotations.EmbeddableInstantiator.class );
		if ( propertyAnnotation != null ) {
			return propertyAnnotation.value();
		}

		final org.hibernate.annotations.EmbeddableInstantiator classAnnotation =
				returnedClass.getAnnotation( org.hibernate.annotations.EmbeddableInstantiator.class );
		if ( classAnnotation != null ) {
			return classAnnotation.value();
		}

		final Class<?> embeddableClass = context.getBootstrapContext().getReflectionManager().toClass( returnedClass );
		if ( embeddableClass != null ) {
			return context.getMetadataCollector().findRegisteredEmbeddableInstantiator( embeddableClass );
		}

		return null;
	}

}
