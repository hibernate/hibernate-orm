/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.annotations.EmbeddedColumnNaming;
import org.hibernate.annotations.Instantiator;
import org.hibernate.annotations.TypeBinderType;
import org.hibernate.binder.TypeBinder;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.MutableInteger;
import org.hibernate.internal.util.NullnessHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.property.access.internal.PropertyAccessStrategyCompositeUserTypeImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyGetterImpl;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.type.BasicType;
import org.hibernate.usertype.CompositeUserType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.hibernate.boot.model.internal.AnnotatedDiscriminatorColumn.DEFAULT_DISCRIMINATOR_COLUMN_NAME;
import static org.hibernate.boot.model.internal.AnnotatedDiscriminatorColumn.buildDiscriminatorColumn;
import static org.hibernate.boot.model.internal.BinderHelper.getPath;
import static org.hibernate.boot.model.internal.BinderHelper.getRelativePath;
import static org.hibernate.boot.model.internal.BinderHelper.hasToOneAnnotation;
import static org.hibernate.boot.model.internal.DialectOverridesAnnotationHelper.getOverridableAnnotation;
import static org.hibernate.boot.model.internal.GeneratorBinder.createIdGeneratorsFromGeneratorAnnotations;
import static org.hibernate.boot.model.internal.PropertyBinder.addElementsOfClass;
import static org.hibernate.boot.model.internal.PropertyBinder.processElementAnnotations;
import static org.hibernate.boot.model.internal.PropertyHolderBuilder.buildPropertyHolder;
import static org.hibernate.internal.CoreLogging.messageLogger;
import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.internal.util.StringHelper.unqualify;
import static org.hibernate.internal.util.collections.CollectionHelper.mapOfSize;

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
			boolean isId,
			boolean isOverridden,
			PropertyData mapsIdProperty,
			Class<? extends CompositeUserType<?>> compositeUserType) {
		return createEmbeddedProperty(
				inferredData,
				propertyHolder,
				entityBinder,
				context,
				isComponentEmbedded,
				isId,
				inheritanceStatePerClass,
				createEmbeddable(
						propertyHolder,
						inferredData,
						entityBinder,
						isIdentifierMapper,
						isComponentEmbedded,
						context,
						inheritanceStatePerClass,
						property,
						columns,
						returnedClass,
						isId,
						isOverridden,
						mapsIdProperty,
						compositeUserType
				)
		);
	}

	private static Component createEmbeddable(
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
			boolean isId,
			boolean isOverridden,
			PropertyData mapsIdProperty,
			Class<? extends CompositeUserType<?>> compositeUserType) {
		if ( isOverridden ) {
			if ( compositeUserType != null ) {
				// I suppose this assertion is correct, but it might not be
				// Perhaps it was OK that we were just ignoring the CUT
				throw new AssertionFailure( "CompositeUserType not allowed with @MapsId" );
			}
			return bindOverriddenEmbeddable(
					propertyHolder,
					inferredData,
					isIdentifierMapper,
					isComponentEmbedded,
					context,
					inheritanceStatePerClass,
					property,
					columns,
					returnedClass,
					isId,
					mapsIdProperty
			);
		}
		else {
			return bindEmbeddable(
					inferredData,
					propertyHolder,
					entityBinder.getPropertyAccessor( property ),
					entityBinder,
					isIdentifierMapper,
					context,
					isComponentEmbedded,
					isId,
					inheritanceStatePerClass,
					determineCustomInstantiator( property, returnedClass, context ),
					compositeUserType,
					columns
			);
		}
	}

	private static Component bindOverriddenEmbeddable(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			boolean isIdentifierMapper,
			boolean isComponentEmbedded,
			MetadataBuildingContext context,
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass,
			MemberDetails property,
			AnnotatedColumns columns,
			ClassDetails returnedClass,
			boolean isId,
			PropertyData mapsIdProperty) {
		// careful: not always a @MapsId property, sometimes it's from an @IdClass
		final String propertyName = mapsIdProperty.getPropertyName();
		final var actualColumns = new AnnotatedJoinColumns();
		actualColumns.setBuildingContext( context );
		actualColumns.setPropertyHolder( propertyHolder );
		actualColumns.setPropertyName( getRelativePath( propertyHolder, propertyName ) );
		//TODO: resetting the parent here looks like a dangerous thing to do
		//      should we be cloning them first (the legacy code did not)
		for ( AnnotatedColumn column : columns.getColumns() ) {
			column.setParent( actualColumns );
		}
		return bindOverriddenEmbeddable(
				inferredData,
				propertyHolder,
				isIdentifierMapper,
				context,
				isComponentEmbedded,
				isId,
				inheritanceStatePerClass,
				mapsIdProperty.getClassOrElementName(),
				propertyName,
				determineCustomInstantiator( property, returnedClass, context ),
				actualColumns
		);
	}

	static boolean isEmbedded(MemberDetails memberDetails, ClassDetails returnedClass) {
		return memberDetails.hasDirectAnnotationUsage( Embedded.class )
			|| memberDetails.hasDirectAnnotationUsage( EmbeddedId.class )
			|| returnedClass.hasDirectAnnotationUsage( Embeddable.class )
				&& !memberDetails.hasDirectAnnotationUsage( Convert.class );
	}

	static boolean isEmbedded(MemberDetails memberDetails, TypeDetails returnedClass) {
		if ( memberDetails.hasDirectAnnotationUsage( Embedded.class )
			|| memberDetails.hasDirectAnnotationUsage( EmbeddedId.class ) ) {
			return true;
		}
		else {
			final var returnClassDetails = returnedClass.determineRawClass();
			return returnClassDetails.hasDirectAnnotationUsage( Embeddable.class )
				&& !memberDetails.hasDirectAnnotationUsage( Convert.class );
		}
	}

	private static Component bindOverriddenEmbeddable(
			PropertyData inferredData,
			PropertyHolder propertyHolder,
			boolean isIdentifierMapper,
			MetadataBuildingContext context,
			boolean isComponentEmbedded,
			boolean isId, // is an identifier
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass,
			String referencedEntityName, // is a embeddable which is overridden by a @MapsId
			String propertyName,
			Class<? extends EmbeddableInstantiator> customInstantiatorImpl,
			AnnotatedJoinColumns annotatedJoinColumns) {
		final var embeddable = createEmbeddable(
				propertyHolder,
				inferredData,
				isComponentEmbedded,
				isIdentifierMapper,
				customInstantiatorImpl,
				context
		);
		context.getMetadataCollector()
				.addSecondPass( new CopyIdentifierComponentSecondPass(
						embeddable,
						referencedEntityName,
						propertyName,
						annotatedJoinColumns,
						context
				) );

		if ( isId ) {
			embeddable.setKey( true );
			checkEmbeddedId( inferredData, propertyHolder, referencedEntityName, embeddable );
		}
		callTypeBinders( embeddable, context, inferredData.getPropertyType() );
		return embeddable;
	}

	static Component bindEmbeddable(
			PropertyData inferredData,
			PropertyHolder propertyHolder,
			AccessType propertyAccessor,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			MetadataBuildingContext context,
			boolean isComponentEmbedded,
			boolean isId, //is an identifier
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass,
			Class<? extends EmbeddableInstantiator> customInstantiatorImpl,
			Class<? extends CompositeUserType<?>> compositeUserTypeClass,
			AnnotatedColumns annotatedColumns) {
		final var embeddable = fillEmbeddable(
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
		if ( isId ) {
			embeddable.setKey( true );
			checkEmbeddedId( inferredData, propertyHolder, null, embeddable );
		}
		callTypeBinders( embeddable, context, inferredData.getPropertyType() );
		return embeddable;
	}

	private static void callTypeBinders(Component embeddable, MetadataBuildingContext context, TypeDetails annotatedClass ) {
		final var metaAnnotatedAnnotations =
				annotatedClass.determineRawClass()
						.getMetaAnnotated( TypeBinderType.class,
								context.getBootstrapContext().getModelsContext() );
		for ( var metaAnnotated : metaAnnotatedAnnotations ) {
			final var binderType = metaAnnotated.annotationType().getAnnotation( TypeBinderType.class );
			try {
				//noinspection rawtypes
				final TypeBinder binder = binderType.binder().getDeclaredConstructor().newInstance();
				//noinspection unchecked
				binder.bind( metaAnnotated, context, embeddable );
			}
			catch (Exception e) {
				throw new AnnotationException(
						"error processing @TypeBinderType annotation '" + metaAnnotated + "'", e );
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
			Component embeddable) {
		final var binder = new PropertyBinder();
		binder.setDeclaringClass( inferredData.getDeclaringClass() );
		binder.setName( inferredData.getPropertyName() );
		binder.setValue( embeddable );
		binder.setMemberDetails( inferredData.getAttributeMember() );
		binder.setAccessType( inferredData.getDefaultAccess() );
		binder.setEmbedded( isComponentEmbedded );
		binder.setHolder( propertyHolder );
		binder.setId( isId );
		binder.setEntityBinder( entityBinder );
		binder.setInheritanceStatePerClass( inheritanceStatePerClass );
		binder.setBuildingContext( context );
		binder.makePropertyAndBind();
		return binder;
	}

	private static void checkEmbeddedId(
			PropertyData inferredData,
			PropertyHolder propertyHolder,
			String referencedEntityName,
			Component embeddable) {
		if ( propertyHolder.getPersistentClass().getIdentifier() != null ) {
			throw new AnnotationException(
					"Embeddable class '" + embeddable.getComponentClassName()
							+ "' may not have a property annotated '@Id' since it is used by '"
							+ getPath(propertyHolder, inferredData)
							+ "' as an '@EmbeddedId'"
			);
		}
		if ( referencedEntityName == null && embeddable.getPropertySpan() == 0 ) {
			throw new AnnotationException(
					"Embeddable class '" + embeddable.getComponentClassName()
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
		final var embeddable = createEmbeddable(
				propertyHolder,
				inferredData,
				isComponentEmbedded,
				isIdentifierMapper,
				customInstantiatorImpl,
				context
		);

		final String subpath = getPath( propertyHolder, inferredData );
		if ( LOG.isTraceEnabled() ) {
			LOG.trace( "Binding embeddable with path: " + subpath );
		}
		final var subholder = buildPropertyHolder(
				embeddable,
				subpath,
				inferredData,
				propertyHolder,
				context,
				inheritanceStatePerClass
		);

		// propertyHolder here is the owner of the embeddable property.
		// Tell it we are about to start the embeddable...
		propertyHolder.startingProperty( inferredData.getAttributeMember() );

		final CompositeUserType<?> compositeUserType;
		final ClassDetails returnedClassOrElement;
		if ( compositeUserTypeClass == null ) {
			compositeUserType = null;
			returnedClassOrElement = inferredData.getClassOrElementType().determineRawClass();
		}
		else {
			compositeUserType = compositeUserType( compositeUserTypeClass, context );
			embeddable.setTypeName( compositeUserTypeClass.getName() );
			returnedClassOrElement = context.getBootstrapContext().getModelsContext().getClassDetailsRegistry().resolveClassDetails( compositeUserType.embeddable().getName() );
		}
		AggregateComponentBinder.processAggregate(
				embeddable,
				propertyHolder,
				inferredData,
				returnedClassOrElement,
				columns,
				context
		);

		final var inheritanceState = inheritanceStatePerClass.get( returnedClassOrElement );
		if ( inheritanceState != null ) {
			inheritanceState.postProcess( embeddable );
			// Main entry point for binding embeddable inheritance
			bindDiscriminator(
					embeddable,
					returnedClassOrElement,
					propertyHolder,
					subholder,
					inferredData,
					inheritanceState,
					context
			);
		}

		final var annotatedTypeDetails = inferredData.getPropertyType();

		final Map<String, String> subclassToSuperclass =
				embeddable.isPolymorphic() ? new HashMap<>() : null;
		final var classElements = collectClassElements(
				propertyAccessor,
				context,
				returnedClassOrElement,
				annotatedTypeDetails,
				isIdClass,
				subclassToSuperclass
		);

		if ( embeddable.isPolymorphic() ) {
			validateInheritanceIsSupported( subholder, compositeUserType );
			final var discriminatorType = (BasicType<?>) embeddable.getDiscriminator().getType();
			// Discriminator values are used to construct the embeddable domain
			// type hierarchy so order of processing is important
			final Map<Object, String> discriminatorValues = new LinkedHashMap<>();
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
			embeddable.setDiscriminatorValues( discriminatorValues );
			embeddable.setSubclassToSuperclass( subclassToSuperclass );
		}

		final var baseClassElements =
				collectBaseClassElements( baseInferredData, propertyAccessor, context, entityAtStake );
		if ( baseClassElements != null
				//useful to avoid breaking pre JPA 2 mappings
				&& !hasAnnotationsOnIdClass( annotatedTypeDetails ) ) {
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

			final var memberDetails = propertyAnnotatedElement.getAttributeMember();
			if ( isIdClass || subholder.isOrWithinEmbeddedId() ) {
				final var property = findProperty( embeddable, memberDetails.getName() );
				if ( property != null ) {
					// Identifier properties are always simple values
					final var value = (SimpleValue) property.getValue();
					createIdGeneratorsFromGeneratorAnnotations(
							subholder,
							propertyAnnotatedElement,
							value,
							context
					);
				}
			}
			else if ( memberDetails.hasDirectAnnotationUsage( GeneratedValue.class ) ) {
				throw new AnnotationException(
						"Property '" + memberDetails.getName() + "' of '"
								+ getPath( propertyHolder, inferredData )
								+ "' is annotated '@GeneratedValue' but is not part of an identifier" );
			}
		}

		if ( compositeUserType != null ) {
			processCompositeUserType( embeddable, compositeUserType );
		}

		return embeddable;
	}

	private static Property findProperty(Component embeddable, String name) {
		for ( var property : embeddable.getProperties() ) {
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

		return context.getBootstrapContext().getManagedBeanRegistry()
				.getBean( compositeUserTypeClass )
				.getBeanInstance();
	}

	private static void bindDiscriminator(
			Component embeddable,
			ClassDetails componentClass,
			PropertyHolder parentHolder,
			PropertyHolder holder,
			PropertyData propertyData,
			InheritanceState inheritanceState,
			MetadataBuildingContext context) {
		if ( inheritanceState != null ) {
			final var discriminatorColumn = processEmbeddableDiscriminatorProperties(
					componentClass,
					propertyData,
					parentHolder,
					holder,
					inheritanceState,
					context
			);
			if ( discriminatorColumn != null ) {
				bindDiscriminatorColumnToComponent( embeddable, discriminatorColumn, holder, context );
			}
		}
	}

	private static AnnotatedDiscriminatorColumn processEmbeddableDiscriminatorProperties(
			ClassDetails classDetails,
			PropertyData propertyData,
			PropertyHolder parentHolder,
			PropertyHolder holder,
			InheritanceState inheritanceState,
			MetadataBuildingContext context) {
		final var discriminatorColumn = classDetails.getDirectAnnotationUsage( DiscriminatorColumn.class );
		final var discriminatorFormula = getOverridableAnnotation( classDetails, DiscriminatorFormula.class, context );
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
						classDetails.getName()
				) );
			}
			if ( discriminatorFormula != null ) {
				throw new AnnotationException( String.format(
						"Embeddable class '%s' is annotated '@DiscriminatorFormula' but it is not the root of the inheritance hierarchy",
						classDetails.getName()
				) );
			}
		}
		return null;
	}

	private static void bindDiscriminatorColumnToComponent(
			Component embeddable,
			AnnotatedDiscriminatorColumn discriminatorColumn,
			PropertyHolder holder,
			MetadataBuildingContext context) {
		assert embeddable.getDiscriminator() == null;
		final var columns = new AnnotatedColumns();
		columns.setPropertyHolder( holder );
		columns.setBuildingContext( context );
		discriminatorColumn.setParent( columns );
		final var discriminatorColumnBinding = new BasicValue( context, embeddable.getTable() );
		discriminatorColumnBinding.setAggregateColumn( embeddable.getAggregateColumn() );
		embeddable.setDiscriminator( discriminatorColumnBinding );
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
		final var container = new PropertyContainer( returnedClassOrElement, annotatedClass, propertyAccessor );
		addElementsOfClass( classElements, container, context, 0 );
		//add elements of the embeddable's mapped superclasses
		ClassDetails subclass = returnedClassOrElement;
		ClassDetails superClass;
		while ( isValidSuperclass( superClass = subclass.getSuperClass(), isIdClass ) ) {
			//FIXME: proper support of type variables incl var resolved at upper levels
			final var superContainer = new PropertyContainer( superClass, annotatedClass, propertyAccessor );
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
			ClassDetails superclass,
			List<PropertyData> classElements,
			BasicType<?> discriminatorType,
			Map<Object, String> discriminatorValues,
			Map<String, String> subclassToSuperclass) {
		for ( var subclass : context.getMetadataCollector().getEmbeddableSubclasses( superclass ) ) {
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
			final var superContainer = new PropertyContainer( subclass, superclass, propertyAccessor );
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
			ClassDetails annotatedClass,
			BasicType<?> discriminatorType,
			Map<Object, String> discriminatorValues) {
		final String explicitValue =
				annotatedClass.hasDirectAnnotationUsage( DiscriminatorValue.class )
						? annotatedClass.getDirectAnnotationUsage( DiscriminatorValue.class ).value()
						: null;
		final String discriminatorValue;
		if ( isBlank( explicitValue ) ) {
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
			|| isIdClass
				&& !superClass.getName().equals( Object.class.getName() )
				&& !superClass.getName().equals( "java.lang.Record" );
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
				final var container =
						new PropertyContainer( baseReturnedClassOrElement.determineRawClass(),
								entityAtStake, propertyAccessor );
				addElementsOfClass( baseClassElements, container, context, 0 );
				baseReturnedClassOrElement = baseReturnedClassOrElement.determineRawClass().getGenericSuperType();
			}
			return baseClassElements;
		}
		else {
			return null;
		}
	}

	private static void processCompositeUserType(Component embeddable, CompositeUserType<?> compositeUserType) {
		embeddable.sortProperties();
		final List<String> sortedPropertyNames = new ArrayList<>( embeddable.getPropertySpan() );
		final List<Type> sortedPropertyTypes = new ArrayList<>( embeddable.getPropertySpan() );
		final var strategy = new PropertyAccessStrategyCompositeUserTypeImpl(
				compositeUserType,
				sortedPropertyNames,
				sortedPropertyTypes
		);
		for ( var property : embeddable.getProperties() ) {
			sortedPropertyNames.add( property.getName() );
			sortedPropertyTypes.add(
					PropertyAccessStrategyGetterImpl.INSTANCE.buildPropertyAccess(
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
		for ( var field : idClass.getFields() ) {
			if ( hasTriggeringAnnotation( field ) ) {
				return true;
			}
		}
		for ( var method : idClass.getMethods() ) {
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
			final String propertyName = idClassPropertyData.getPropertyName();
			final PropertyData entityPropertyData = baseClassElementsByName.get( propertyName );
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
				if ( !hasCompatibleType( idClassPropertyData.getTypeName(), entityPropertyData.getTypeName() ) ) {
					throw new AnnotationException(
							"Property '" + propertyName + "' in @IdClass '" + idClassPropertyData.getDeclaringClass().getName()
									+ "' doesn't match type in entity class '" + baseInferredData.getPropertyType().getName()
									+ "' (expected '" + entityPropertyData.getTypeName() + "' but was '" + idClassPropertyData.getTypeName() + "')"
					);
				}
			}
			classElements.set( i, entityPropertyData );  //this works since they are in the same order
		}
	}

	private static boolean hasCompatibleType(String typeNameInIdClass, String typeNameInEntityClass) {
		return typeNameInIdClass.equals( typeNameInEntityClass )
			|| canonicalize( typeNameInIdClass ).equals( typeNameInEntityClass )
			|| typeNameInIdClass.equals( canonicalize( typeNameInEntityClass ) );
	}

	private static String canonicalize(String typeName) {
		return switch (typeName) {
			case "boolean" -> Boolean.class.getName();
			case "char" -> Character.class.getName();
			case "int" -> Integer.class.getName();
			case "long" -> Long.class.getName();
			case "short" -> Short.class.getName();
			case "byte" -> Byte.class.getName();
			case "float" -> Float.class.getName();
			case "double" -> Double.class.getName();
			default -> typeName;
		};
	}

	static Component createEmbeddable(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			boolean isComponentEmbedded,
			boolean isIdentifierMapper,
			Class<? extends EmbeddableInstantiator> customInstantiatorImpl,
			MetadataBuildingContext context) {
		final var embeddable = new Component( context, propertyHolder.getPersistentClass() );
		embeddable.setEmbedded( isComponentEmbedded );
		//yuk
		embeddable.setTable( propertyHolder.getTable() );
		if ( isIdentifierMapper
				|| isComponentEmbedded && inferredData.getPropertyName() == null ) {
			embeddable.setComponentClassName( embeddable.getOwner().getClassName() );
		}
		else {
			embeddable.setComponentClassName( inferredData.getClassOrElementType().getName() );
		}
		embeddable.setCustomInstantiator( customInstantiatorImpl );
		final var constructor = resolveInstantiator( inferredData.getClassOrElementType() );
		if ( constructor != null ) {
			embeddable.setInstantiator( constructor, constructor.getAnnotation( Instantiator.class ).value() );
		}
		if ( propertyHolder.isComponent() ) {
			final var componentPropertyHolder = (ComponentPropertyHolder) propertyHolder;
			embeddable.setParentAggregateColumn( componentPropertyHolder.getAggregateColumn() );
		}
		applyColumnNamingPattern( embeddable, inferredData );
		return embeddable;
	}

	private static void applyColumnNamingPattern(Component embeddable, PropertyData inferredData) {
		final var componentClass = embeddable.getComponentClass();
		if ( componentClass == null || Map.class.equals( componentClass ) ) {
			// dynamic models
			return;
		}

		if ( inferredData.getAttributeMember() == null ) {
			// generally indicates parts of a plural mapping (element, key, bag-id)
			return;
		}

		final var columnNaming =
				inferredData.getAttributeMember().getDirectAnnotationUsage( EmbeddedColumnNaming.class );
		if ( columnNaming == null ) {
			return;
		}

		final String columnNamingPattern = NullnessHelper.coalesce(
				columnNaming.value(),
				inferredData.getPropertyName() + "_%s"
		);

		final int markerCount = StringHelper.count( columnNamingPattern, '%' );
		if ( markerCount != 1 ) {
			throw new MappingException( String.format(
					Locale.ROOT,
					"@EmbeddedColumnNaming expects pattern with exactly 1 format maker, but found %s - `%s` (%s#%s)",
					markerCount,
					columnNamingPattern,
					inferredData.getAttributeMember().getDeclaringType().getName(),
					inferredData.getAttributeMember().getName()
			) );
		}

		embeddable.setColumnNamingPattern( columnNamingPattern );
	}

	private static Constructor<?> resolveInstantiator(TypeDetails embeddableClass) {
		return embeddableClass == null ? null : resolveInstantiator( embeddableClass.determineRawClass() );
	}

	private static Constructor<?> resolveInstantiator(ClassDetails embeddableClass) {
		if ( embeddableClass != null ) {
			final var declaredConstructors = embeddableClass.toJavaClass().getDeclaredConstructors();
			Constructor<?> constructor = null;
			for ( var declaredConstructor : declaredConstructors ) {
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

		final var propertyAnnotation =
				property.getDirectAnnotationUsage( org.hibernate.annotations.EmbeddableInstantiator.class );
		if ( propertyAnnotation != null ) {
			return propertyAnnotation.value();
		}

		final var classAnnotation =
				returnedClass.getDirectAnnotationUsage( org.hibernate.annotations.EmbeddableInstantiator.class );
		if ( classAnnotation != null ) {
			return classAnnotation.value();
		}

		if ( returnedClass.getClassName() != null ) {
			return context.getMetadataCollector().findRegisteredEmbeddableInstantiator( returnedClass.toJavaClass() );
		}

		return null;
	}

	private static class CopyIdentifierComponentSecondPass implements FkSecondPass {
		private final String referencedEntityName;
		private final String propertyName;
		private final Component embeddable;
		private final MetadataBuildingContext buildingContext;
		private final AnnotatedJoinColumns joinColumns;

		private CopyIdentifierComponentSecondPass(
				Component embeddable,
				String referencedEntityName, String propertyName,
				AnnotatedJoinColumns joinColumns,
				MetadataBuildingContext buildingContext) {
			this.embeddable = embeddable;
			this.referencedEntityName = referencedEntityName;
			this.propertyName = propertyName;
			this.buildingContext = buildingContext;
			this.joinColumns = joinColumns;
		}

		@Override
		public Value getValue() {
			return embeddable;
		}

		@Override
		public String getReferencedEntityName() {
			return referencedEntityName;
		}

		@Override
		public boolean isInPrimaryKey() {
			// This second pass is apparently only ever used to initialize composite identifiers
			return true;
		}

		@Override
		public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
			final var referencedPersistentClass = persistentClasses.get( referencedEntityName );
			final var referencedComponent = getReferencedComponent( referencedPersistentClass );

			//prepare column name structure
			boolean isExplicitReference = true;
			final List<AnnotatedJoinColumn> columns = joinColumns.getJoinColumns();
			final Map<String, AnnotatedJoinColumn> columnByReferencedName = mapOfSize( columns.size() );
			for ( AnnotatedJoinColumn joinColumn : columns ) {
				if ( !joinColumn.isReferenceImplicit() ) {
					//JPA 2 requires referencedColumnNames to be case-insensitive
					columnByReferencedName.put( joinColumn.getReferencedColumn().toLowerCase( Locale.ROOT), joinColumn );
				}
			}
			//try default column orientation
			if ( columnByReferencedName.isEmpty() ) {
				isExplicitReference = false;
				for (int i = 0; i < columns.size(); i++ ) {
					columnByReferencedName.put( String.valueOf( i ), columns.get(i) );
				}
			}

			final MutableInteger index = new MutableInteger();
			for ( Property referencedProperty : referencedComponent.getProperties() ) {
				final Property property;
				if ( referencedProperty.isComposite() ) {
					property = createComponentProperty(
							isExplicitReference,
							columnByReferencedName,
							index,
							referencedProperty
					);
				}
				else {
					property = createSimpleProperty(
							referencedPersistentClass,
							isExplicitReference,
							columnByReferencedName,
							index,
							referencedProperty
					);
				}
				embeddable.addProperty( property );
			}
		}

		private Component getReferencedComponent(PersistentClass referencedPersistentClass) {
			if ( referencedPersistentClass == null ) {
				// TODO: much better error message if this is something that can really happen!
				throw new AnnotationException( "Unknown entity name '" + referencedEntityName + "'");
			}
			if ( referencedPersistentClass.getIdentifier() instanceof Component id ) {
				return id;
			}
			else {
				// The entity with the @MapsId annotation has a composite
				// id type, but the referenced entity has a basic-typed id.
				// Therefore, the @MapsId annotation should have specified
				// a property of the composite id that has the foreign key
				throw new AnnotationException(
						"Missing 'value' in '@MapsId' annotation of association '" + propertyName
						+ "' of entity '" + embeddable.getOwner().getEntityName()
						+ "' with composite identifier type"
						+ " ('@MapsId' must specify a property of the '@EmbeddedId' class which has the foreign key of '"
						+ referencedEntityName + "')"
				);
			}
		}

		private Property createComponentProperty(
				boolean isExplicitReference,
				Map<String, AnnotatedJoinColumn> columnByReferencedName,
				MutableInteger index,
				Property referencedProperty ) {
			final var property = new Property();
			property.setName( referencedProperty.getName() );
			//FIXME set optional?
			//property.setOptional( property.isOptional() );
			property.setPersistentClass( embeddable.getOwner() );
			property.setPropertyAccessorName( referencedProperty.getPropertyAccessorName() );
			var value = new Component( buildingContext, embeddable.getOwner() );

			property.setValue( value );
			final var referencedValue = (Component) referencedProperty.getValue();
			value.setTypeName( referencedValue.getTypeName() );
			value.setTypeParameters( referencedValue.getTypeParameters() );
			value.setComponentClassName( referencedValue.getComponentClassName() );


			for ( var referencedComponentProperty : referencedValue.getProperties() ) {
				if ( referencedComponentProperty.isComposite() ) {
					value.addProperty( createComponentProperty(
							isExplicitReference,
							columnByReferencedName,
							index,
							referencedComponentProperty
					) );
				}
				else {
					value.addProperty( createSimpleProperty(
							referencedValue.getOwner(),
							isExplicitReference,
							columnByReferencedName,
							index,
							referencedComponentProperty
					) );
				}
			}

			return property;
		}


		private Property createSimpleProperty(
				PersistentClass referencedPersistentClass,
				boolean isExplicitReference,
				Map<String, AnnotatedJoinColumn> columnByReferencedName,
				MutableInteger index,
				Property referencedProperty ) {
			final var property = new Property();
			property.setName( referencedProperty.getName() );
			//FIXME set optional?
			//property.setOptional( property.isOptional() );
			property.setPersistentClass( embeddable.getOwner() );
			property.setPropertyAccessorName( referencedProperty.getPropertyAccessorName() );
			final var value = new BasicValue( buildingContext, embeddable.getTable() );
			property.setValue( value );
			final var referencedValue = (SimpleValue) referencedProperty.getValue();
			value.copyTypeFrom( referencedValue );

			//TODO: this bit is nasty, move up to AnnotatedJoinColumns
			final AnnotatedJoinColumn firstColumn = joinColumns.getJoinColumns().get(0);
			if ( firstColumn.isNameDeferred() ) {
				firstColumn.copyReferencedStructureAndCreateDefaultJoinColumns(
						referencedPersistentClass,
						referencedValue,
						value
				);
			}
			else {
				for ( var selectable : referencedValue.getSelectables() ) {
					if ( selectable instanceof org.hibernate.mapping.Column column ) {
						final AnnotatedJoinColumn joinColumn;
						final String logicalColumnName;
						if ( isExplicitReference ) {
							logicalColumnName = column.getName();
							//JPA 2 requires referencedColumnNames to be case-insensitive
							joinColumn = columnByReferencedName.get( logicalColumnName.toLowerCase( Locale.ROOT ) );
						}
						else {
							logicalColumnName = null;
							joinColumn = columnByReferencedName.get( String.valueOf( index.get() ) );
							index.getAndIncrement();
						}
						if ( joinColumn == null && !firstColumn.isNameDeferred() ) {
							throw new AnnotationException(
									"Property '" + propertyName
									+ "' of entity '" + embeddable.getOwner().getEntityName()
									+ "' must have a '@JoinColumn' which references the foreign key column '"
									+ logicalColumnName + "'"
							);
						}
						final String columnName =
								joinColumn == null || joinColumn.isNameDeferred()
										? "tata_" + column.getName()
										: joinColumn.getName();

						final var database = buildingContext.getMetadataCollector().getDatabase();
						final String physicalName =
								buildingContext.getBuildingOptions().getPhysicalNamingStrategy()
										.toPhysicalColumnName( database.toIdentifier( columnName ),
												database.getJdbcEnvironment() )
										.render( database.getDialect() );
						value.addColumn( new org.hibernate.mapping.Column( physicalName ) );
						if ( joinColumn != null ) {
							applyComponentColumnSizeValueToJoinColumn( column, joinColumn );
							joinColumn.linkWithValue( value );
						}
						column.setValue( value );
					}
					else {
						//FIXME take care of Formula
					}
				}
			}
			return property;
		}

		private void applyComponentColumnSizeValueToJoinColumn(
				org.hibernate.mapping.Column column, AnnotatedJoinColumn joinColumn) {
			final var mappingColumn = joinColumn.getMappingColumn();
			mappingColumn.setLength( column.getLength() );
			mappingColumn.setPrecision( column.getPrecision() );
			mappingColumn.setScale( column.getScale() );
			mappingColumn.setArrayLength( column.getArrayLength() );
		}
	}
}
