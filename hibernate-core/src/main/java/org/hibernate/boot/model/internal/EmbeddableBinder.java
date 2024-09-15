/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

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
import org.hibernate.AnnotationException;
import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.annotations.Instantiator;
import org.hibernate.annotations.TypeBinderType;
import org.hibernate.binder.TypeBinder;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.MethodDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.property.access.internal.PropertyAccessStrategyCompositeUserTypeImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyMixedImpl;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.usertype.CompositeUserType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hibernate.boot.model.internal.AnnotatedDiscriminatorColumn.DEFAULT_DISCRIMINATOR_COLUMN_NAME;
import static org.hibernate.boot.model.internal.AnnotatedDiscriminatorColumn.buildDiscriminatorColumn;
import static org.hibernate.boot.model.internal.BinderHelper.getPath;
import static org.hibernate.boot.model.internal.BinderHelper.getPropertyOverriddenByMapperOrMapsId;
import static org.hibernate.boot.model.internal.BinderHelper.getRelativePath;
import static org.hibernate.boot.model.internal.BinderHelper.hasToOneAnnotation;
import static org.hibernate.boot.model.internal.DialectOverridesAnnotationHelper.getOverridableAnnotation;
import static org.hibernate.boot.model.internal.GeneratorBinder.createIdGeneratorsFromGeneratorAnnotations;
import static org.hibernate.boot.model.internal.PropertyBinder.addElementsOfClass;
import static org.hibernate.boot.model.internal.PropertyBinder.processElementAnnotations;
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
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass,
			MemberDetails property,
			AnnotatedColumns columns,
			ClassDetails returnedClass,
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
					property.resolveAttributeName(),
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

	static boolean isEmbedded(MemberDetails property, ClassDetails returnedClass) {
		return property.hasDirectAnnotationUsage( Embedded.class )
			|| property.hasDirectAnnotationUsage( EmbeddedId.class )
			|| returnedClass.hasDirectAnnotationUsage( Embeddable.class ) && !property.hasDirectAnnotationUsage( Convert.class );
	}

	static boolean isEmbedded(MemberDetails property, TypeDetails returnedClass) {
		if ( property.hasDirectAnnotationUsage( Embedded.class ) || property.hasDirectAnnotationUsage( EmbeddedId.class ) ) {
			return true;
		}

		final ClassDetails returnClassDetails = returnedClass.determineRawClass();
		return returnClassDetails.hasDirectAnnotationUsage( Embeddable.class )
				&& !property.hasDirectAnnotationUsage( Convert.class );
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
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass,
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
		callTypeBinders( component, context, inferredData.getPropertyType() );
		return component;
	}

	private static void callTypeBinders(Component component, MetadataBuildingContext context, TypeDetails annotatedClass ) {
		final SourceModelBuildingContext sourceModelContext = context.getMetadataCollector().getSourceModelBuildingContext();

		final List<? extends Annotation> metaAnnotatedAnnotations = annotatedClass.determineRawClass().getMetaAnnotated( TypeBinderType.class, sourceModelContext );
		if ( CollectionHelper.isEmpty( metaAnnotatedAnnotations ) ) {
			return;
		}

		for ( Annotation metaAnnotated : metaAnnotatedAnnotations ) {
			final TypeBinderType binderType = metaAnnotated.annotationType().getAnnotation( TypeBinderType.class );
			try {
				//noinspection rawtypes
				final Class<? extends TypeBinder> binderImpl = binderType.binder();
				//noinspection rawtypes
				final TypeBinder binder = binderImpl.getDeclaredConstructor().newInstance();
				//noinspection unchecked
				binder.bind( metaAnnotated, context, component );
			}
			catch ( Exception e ) {
				throw new AnnotationException( "error processing @TypeBinderType annotation '" + metaAnnotated + "'", e );
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
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass,
			Component component) {
		final PropertyBinder binder = new PropertyBinder();
		binder.setDeclaringClass( inferredData.getDeclaringClass() );
		binder.setName( inferredData.getPropertyName() );
		binder.setValue(component);
		binder.setMemberDetails( inferredData.getAttributeMember() );
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
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass) {
		return fillEmbeddable(
				propertyHolder,
				inferredData,
				null,
				propertyAccessor,
				null,
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
			ClassDetails entityAtStake,
			boolean isNullable,
			EntityBinder entityBinder,
			boolean isComponentEmbedded,
			boolean isIdentifierMapper,
			boolean inSecondPass,
			Class<? extends EmbeddableInstantiator> customInstantiatorImpl,
			Class<? extends CompositeUserType<?>> compositeUserTypeClass,
			AnnotatedColumns columns,
			MetadataBuildingContext context,
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass,
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
		propertyHolder.startingProperty( inferredData.getAttributeMember() );

		final CompositeUserType<?> compositeUserType;
		final ClassDetails returnedClassOrElement;
		if ( compositeUserTypeClass == null ) {
			compositeUserType = null;
			returnedClassOrElement = inferredData.getClassOrElementType().determineRawClass();
		}
		else {
			compositeUserType = compositeUserType( compositeUserTypeClass, context );
			component.setTypeName( compositeUserTypeClass.getName() );
			returnedClassOrElement = context.getMetadataCollector().getSourceModelBuildingContext().getClassDetailsRegistry().resolveClassDetails( compositeUserType.embeddable().getName() );
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

		final Map<String, String> subclassToSuperclass = component.isPolymorphic() ? new HashMap<>() : null;
		final TypeDetails annotatedType = inferredData.getPropertyType();
		final List<PropertyData> classElements = collectClassElements(
				propertyAccessor,
				context,
				returnedClassOrElement,
				annotatedType,
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
				collectBaseClassElements( baseInferredData, propertyAccessor, context, entityAtStake );
		if ( baseClassElements != null
				//useful to avoid breaking pre JPA 2 mappings
				&& !hasAnnotationsOnIdClass( annotatedType ) ) {
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
					entityBinder,
					isIdentifierMapper,
					isComponentEmbedded,
					inSecondPass,
					context,
					inheritanceStatePerClass
			);

			final MemberDetails member = propertyAnnotatedElement.getAttributeMember();
			if ( isIdClass || subholder.isOrWithinEmbeddedId() ) {
				final Property property = findProperty( component, member.getName() );
				if ( property != null ) {
					// Identifier properties are always simple values
					final SimpleValue value = (SimpleValue) property.getValue();
					createIdGeneratorsFromGeneratorAnnotations(
							subholder,
							propertyAnnotatedElement,
							value,
							context
					);
				}
			}
			else if ( member.hasDirectAnnotationUsage( GeneratedValue.class ) ) {
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
			ClassDetails componentClass,
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
			ClassDetails annotatedClass,
			PropertyData propertyData,
			PropertyHolder parentHolder,
			PropertyHolder holder,
			InheritanceState inheritanceState,
			MetadataBuildingContext context) {
		final DiscriminatorColumn discriminatorColumn = annotatedClass.getDirectAnnotationUsage( DiscriminatorColumn.class );
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
			ClassDetails returnedClassOrElement,
			TypeDetails annotatedClass,
			boolean isIdClass,
			Map<String, String> subclassToSuperclass) {
		final List<PropertyData> classElements = new ArrayList<>();
		//embeddable elements can have type defs
		final PropertyContainer container =
				new PropertyContainer( returnedClassOrElement, annotatedClass, propertyAccessor );
		addElementsOfClass( classElements, container, context);
		//add elements of the embeddable's mapped superclasses
		ClassDetails subclass = returnedClassOrElement;
		ClassDetails superClass;
		while ( isValidSuperclass( superClass = subclass.getSuperClass(), isIdClass ) ) {
			//FIXME: proper support of type variables incl var resolved at upper levels
			final PropertyContainer superContainer = new PropertyContainer(
					superClass,
					annotatedClass,
					propertyAccessor
			);
			addElementsOfClass( classElements, superContainer, context );
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
			ClassDetails superclass,
			List<PropertyData> classElements,
			BasicType<?> discriminatorType,
			Map<Object, String> discriminatorValues,
			Map<String, String> subclassToSuperclass) {
		for ( final ClassDetails subclass : context.getMetadataCollector().getEmbeddableSubclasses( superclass ) ) {
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
			addElementsOfClass( classElements, superContainer, context );
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
			ClassDetails annotatedClass,
			BasicType<?> discriminatorType,
			Map<Object, String> discriminatorValues) {
		final String explicitValue = annotatedClass.hasDirectAnnotationUsage( DiscriminatorValue.class )
				? annotatedClass.getDirectAnnotationUsage( DiscriminatorValue.class ).value()
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


	private static boolean isValidSuperclass(ClassDetails superClass, boolean isIdClass) {
		if ( superClass == null ) {
			return false;
		}

		return superClass.hasDirectAnnotationUsage( MappedSuperclass.class )
				|| ( isIdClass
				&& !superClass.getName().equals( Object.class.getName() )
				&& !superClass.getName().equals( "java.lang.Record" ) );
	}

	private static List<PropertyData> collectBaseClassElements(
			PropertyData baseInferredData,
			AccessType propertyAccessor,
			MetadataBuildingContext context,
			ClassDetails entityAtStake) {
		if ( baseInferredData != null ) {
			final List<PropertyData> baseClassElements = new ArrayList<>();
			// iterate from base returned class up hierarchy to handle cases where the @Id attributes
			// might be spread across the subclasses and super classes.
			TypeDetails baseReturnedClassOrElement = baseInferredData.getClassOrElementType();
			while ( !Object.class.getName().equals( baseReturnedClassOrElement.getName() ) ) {
				final PropertyContainer container = new PropertyContainer(
						baseReturnedClassOrElement.determineRawClass(),
						entityAtStake,
						propertyAccessor
				);
				addElementsOfClass( baseClassElements, container, context );
				baseReturnedClassOrElement = baseReturnedClassOrElement.determineRawClass().getGenericSuperType();
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

	private static boolean hasAnnotationsOnIdClass(TypeDetails idClassType) {
		return hasAnnotationsOnIdClass( idClassType.determineRawClass() );
	}
	private static boolean hasAnnotationsOnIdClass(ClassDetails idClass) {
		for ( FieldDetails field : idClass.getFields() ) {
			if ( hasTriggeringAnnotation( field ) ) {
				return true;
			}
		}
		for ( MethodDetails method : idClass.getMethods() ) {
			if ( hasTriggeringAnnotation( method ) ) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasTriggeringAnnotation(MemberDetails property) {
		return property.hasDirectAnnotationUsage(Column.class)
			|| property.hasDirectAnnotationUsage(OneToMany.class)
			|| property.hasDirectAnnotationUsage(ManyToOne.class)
			|| property.hasDirectAnnotationUsage(Id.class)
			|| property.hasDirectAnnotationUsage(GeneratedValue.class)
			|| property.hasDirectAnnotationUsage(OneToOne.class)
			|| property.hasDirectAnnotationUsage(ManyToMany.class);
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
									+ baseInferredData.getPropertyType().getName()
									+ "' (every property of the '@IdClass' must have a corresponding persistent property in the '@Entity' class)"
					);
				}
				if ( hasToOneAnnotation( entityPropertyData.getAttributeMember() )
						&& !entityPropertyData.getClassOrElementType().equals( idClassPropertyData.getClassOrElementType() ) ) {
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
		if ( isIdentifierMapper
				|| isComponentEmbedded && inferredData.getPropertyName() == null ) {
			component.setComponentClassName( component.getOwner().getClassName() );
		}
		else {
			component.setComponentClassName( inferredData.getClassOrElementType().getName() );
		}
		component.setCustomInstantiator( customInstantiatorImpl );
		final Constructor<?> constructor = resolveInstantiator( inferredData.getClassOrElementType() );
		if ( constructor != null ) {
			component.setInstantiator( constructor, constructor.getAnnotation( Instantiator.class ).value() );
		}
		if ( propertyHolder.isComponent() ) {
			final ComponentPropertyHolder componentPropertyHolder = (ComponentPropertyHolder) propertyHolder;
			component.setParentAggregateColumn( componentPropertyHolder.getAggregateColumn() );
		}
		return component;
	}

	private static Constructor<?> resolveInstantiator(TypeDetails embeddableClass) {
		return embeddableClass == null ? null : resolveInstantiator( embeddableClass.determineRawClass() );
	}

	private static Constructor<?> resolveInstantiator(ClassDetails embeddableClass) {
		if ( embeddableClass != null ) {
			final Constructor<?>[] declaredConstructors = embeddableClass.toJavaClass().getDeclaredConstructors();
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
			MemberDetails property,
			ClassDetails returnedClass,
			MetadataBuildingContext context) {
		if ( property.hasDirectAnnotationUsage( EmbeddedId.class ) ) {
			// we don't allow custom instantiators for composite ids
			return null;
		}

		final org.hibernate.annotations.EmbeddableInstantiator propertyAnnotation =
				property.getDirectAnnotationUsage( org.hibernate.annotations.EmbeddableInstantiator.class );
		if ( propertyAnnotation != null ) {
			return propertyAnnotation.value();
		}

		final org.hibernate.annotations.EmbeddableInstantiator classAnnotation =
				returnedClass.getDirectAnnotationUsage( org.hibernate.annotations.EmbeddableInstantiator.class );
		if ( classAnnotation != null ) {
			return classAnnotation.value();
		}

		if ( returnedClass.getClassName() != null ) {
			return context.getMetadataCollector().findRegisteredEmbeddableInstantiator( returnedClass.toJavaClass() );
		}

		return null;
	}

}
