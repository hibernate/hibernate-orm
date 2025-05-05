/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.MapKeyJoinColumns;
import jakarta.persistence.MapKeyTemporal;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Temporal;
import jakarta.persistence.Version;
import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AttributeBinderType;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.Parent;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.models.spi.AnnotationDescriptor;
import org.hibernate.models.spi.AnnotationDescriptorRegistry;
import org.hibernate.models.spi.ArrayTypeDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.models.spi.TypeVariableScope;
import org.hibernate.usertype.CompositeUserType;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static jakarta.persistence.FetchType.LAZY;
import static org.hibernate.boot.model.internal.AnyBinder.bindAny;
import static org.hibernate.boot.model.internal.BinderHelper.getMappedSuperclassOrNull;
import static org.hibernate.boot.model.internal.BinderHelper.getPath;
import static org.hibernate.boot.model.internal.BinderHelper.hasToOneAnnotation;
import static org.hibernate.boot.model.internal.ClassPropertyHolder.handleGenericComponentProperty;
import static org.hibernate.boot.model.internal.ClassPropertyHolder.prepareActualProperty;
import static org.hibernate.boot.model.internal.CollectionBinder.bindCollection;
import static org.hibernate.boot.model.internal.EmbeddableBinder.bindEmbeddable;
import static org.hibernate.boot.model.internal.EmbeddableBinder.createCompositeBinder;
import static org.hibernate.boot.model.internal.EmbeddableBinder.createEmbeddable;
import static org.hibernate.boot.model.internal.EmbeddableBinder.determineCustomInstantiator;
import static org.hibernate.boot.model.internal.EmbeddableBinder.isEmbedded;
import static org.hibernate.boot.model.internal.GeneratorBinder.createIdGeneratorsFromGeneratorAnnotations;
import static org.hibernate.boot.model.internal.GeneratorBinder.createValueGeneratorFromAnnotations;
import static org.hibernate.boot.model.internal.NaturalIdBinder.addNaturalIds;
import static org.hibernate.boot.model.internal.TimeZoneStorageHelper.resolveTimeZoneStorageCompositeUserType;
import static org.hibernate.boot.model.internal.ToOneBinder.bindManyToOne;
import static org.hibernate.boot.model.internal.ToOneBinder.bindOneToOne;
import static org.hibernate.id.IdentifierGeneratorHelper.getForeignId;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

/**
 * A stateful binder responsible for creating {@link Property} objects.
 *
 * @author Emmanuel Bernard
 */
public class PropertyBinder {

	private MetadataBuildingContext buildingContext;

	private String name;
	private String returnedClassName;
	private boolean lazy;
	private String lazyGroup;
	private AccessType accessType;
	private AnnotatedColumns columns;
	private PropertyHolder holder;
	private Value value;
	private Component componentElement;
	private boolean insertable = true;
	private boolean updatable = true;
	private EnumSet<CascadeType> cascadeTypes;
	private BasicValueBinder basicValueBinder;
	private ClassDetails declaringClass;
	private boolean declaringClassSet;
	private boolean embedded;
	private EntityBinder entityBinder;
	private boolean toMany;
	private String referencedEntityName; // only used for @MapsId or @IdClass

	protected ModelsContext getSourceModelContext() {
		return buildingContext.getBootstrapContext().getModelsContext();
	}

	private void setReferencedEntityName(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName;
	}

	public void setEmbedded(boolean embedded) {
		this.embedded = embedded;
	}

	public void setEntityBinder(EntityBinder entityBinder) {
		this.entityBinder = entityBinder;
	}

	// property can be null
	// prefer propertyName to property.getName() since some are overloaded
	private MemberDetails memberDetails;
	private TypeDetails returnedClass;
	private boolean isId;
	private Map<ClassDetails, InheritanceState> inheritanceStatePerClass;

	public void setInsertable(boolean insertable) {
		this.insertable = insertable;
	}

	public void setUpdatable(boolean updatable) {
		this.updatable = updatable;
	}

	public void setName(String name) {
		this.name = name;
	}

	private void setReturnedClassName(String returnedClassName) {
		this.returnedClassName = returnedClassName;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public void setLazyGroup(String lazyGroup) {
		this.lazyGroup = lazyGroup;
	}

	public void setAccessType(AccessType accessType) {
		this.accessType = accessType;
	}

	public void setColumns(AnnotatedColumns columns) {
		//consistency is checked later when we know the property name
		this.columns = columns;
	}

	public void setHolder(PropertyHolder holder) {
		this.holder = holder;
	}

	public void setValue(Value value) {
		this.value = value;
	}

	private void setComponentElement(Component componentElement) {
		this.componentElement = componentElement;
	}

	public void setCascade(EnumSet<CascadeType> cascadeTypes) {
		this.cascadeTypes = cascadeTypes;
	}

	public void setBuildingContext(MetadataBuildingContext buildingContext) {
		this.buildingContext = buildingContext;
	}

	public void setDeclaringClass(ClassDetails declaringClassDetails) {
		this.declaringClass = declaringClassDetails;
		this.declaringClassSet = true;
	}

	private boolean isToOneValue(Value value) {
		return value instanceof ToOne;
	}

	public void setMemberDetails(MemberDetails memberDetails) {
		this.memberDetails = memberDetails;
	}

	private void setReturnedClass(TypeDetails returnedClass) {
		this.returnedClass = returnedClass;
	}

	private BasicValueBinder getBasicValueBinder() {
		return basicValueBinder;
	}

	private Value getValue() {
		return value;
	}

	public void setId(boolean id) {
		this.isId = id;
	}

	public boolean isId() {
		return isId;
	}

	public void setInheritanceStatePerClass(Map<ClassDetails, InheritanceState> inheritanceStatePerClass) {
		this.inheritanceStatePerClass = inheritanceStatePerClass;
	}

	private void validateBind() {
		if ( !declaringClassSet ) {
			throw new AssertionFailure( "declaringClass has not been set before a bind" );
		}
	}

	private void validateMake() {
		//TODO check necessary params for a make
	}

	private Property makePropertyAndValue() {
		validateBind();

		final String containerClassName = holder.getClassName();
		holder.startingProperty( memberDetails );

		basicValueBinder = new BasicValueBinder( BasicValueBinder.Kind.ATTRIBUTE, buildingContext );
		basicValueBinder.setReturnedClassName( returnedClassName );
		basicValueBinder.setColumns( columns );
		basicValueBinder.setPersistentClassName( containerClassName );
		basicValueBinder.setType(
				memberDetails,
				returnedClass,
				containerClassName,
				holder.resolveAttributeConverterDescriptor( memberDetails, autoApplyConverters() )
		);
		basicValueBinder.setReferencedEntityName( referencedEntityName );
		basicValueBinder.setAccessType( accessType );

		value = basicValueBinder.make();

		return makeProperty();
	}

	private boolean autoApplyConverters() {
		// JPA 3.2 section 3.9 says there are exceptions where to auto-apply converters, citing:
		// The conversion of all basic types is supported except for the following:
		// Id attributes (including the attributes of embedded ids and derived identities),
		// version attributes, relationship attributes,
		// and attributes explicitly annotated as Enumerated or Temporal
		return !isId
			&& !isVersion( memberDetails )
			&& !memberDetails.hasDirectAnnotationUsage( Enumerated.class )
			&& !memberDetails.hasDirectAnnotationUsage( Temporal.class );
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void callAttributeBinders(Property property, Map<String, PersistentClass> persistentClasses) {
		final List<? extends Annotation> metaAnnotatedTargets =
				memberDetails.getMetaAnnotated( AttributeBinderType.class, getSourceModelContext() );

		if ( !isEmpty( metaAnnotatedTargets ) ) {
			final AnnotationDescriptorRegistry descriptorRegistry =
					getSourceModelContext().getAnnotationDescriptorRegistry();
			for ( int i = 0; i < metaAnnotatedTargets.size(); i++ ) {
				final Annotation metaAnnotatedTarget = metaAnnotatedTargets.get( i );
				final AnnotationDescriptor<? extends Annotation> metaAnnotatedDescriptor =
						descriptorRegistry.getDescriptor( metaAnnotatedTarget.annotationType() );
				final AttributeBinderType binderTypeAnn =
						metaAnnotatedDescriptor.getDirectAnnotationUsage( AttributeBinderType.class );
				try {
					final AttributeBinder binder = binderTypeAnn.binder().getConstructor().newInstance();
					final PersistentClass persistentClass = entityBinder != null
							? entityBinder.getPersistentClass()
							: persistentClasses.get( holder.getEntityName() );
					binder.bind( metaAnnotatedTarget, buildingContext, persistentClass, property );
				}
				catch ( Exception e ) {
					throw new AnnotationException( "error processing @AttributeBinderType annotation '"
							+ metaAnnotatedDescriptor.getAnnotationType().getName() + "' for property '"
							+ qualify( holder.getPath(), name ) + "'", e );
				}
			}
		}
	}

	//used when value is provided
	public Property makePropertyAndBind() {
		return bind( makeProperty() );
	}

	//used to build everything from scratch
	private Property makePropertyValueAndBind() {
		return bind( makePropertyAndValue() );
	}

	public void setToMany(boolean toMany) {
		this.toMany = toMany;
	}

	private Property bind(Property property) {
		if ( isId ) {
			bindId( property );
		}
		else {
			holder.addProperty( property, memberDetails, columns, declaringClass );
		}
		callAttributeBindersInSecondPass( property );
		return property;
	}

	void callAttributeBindersInSecondPass(Property property) {
		final InFlightMetadataCollector metadataCollector = buildingContext.getMetadataCollector();
		if ( metadataCollector.isInSecondPass() ) {
			callAttributeBinders( property, metadataCollector.getEntityBindingMap() );
		}
		else {
			metadataCollector.addSecondPass( persistentClasses -> callAttributeBinders( property, persistentClasses ) );
		}
	}

	private void bindId(Property property) {
		final RootClass rootClass = (RootClass) holder.getPersistentClass();
		if ( toMany || entityBinder.wrapIdsInEmbeddedComponents() ) {
			// If an XxxToMany, it has to be wrapped today.
			// This poses a problem as the PK is the class instead of the
			// associated class which is not really compliant with the spec
			//FIXME is it good enough?
			getOrCreateCompositeId( rootClass ).addProperty( property );
		}
		else {
			rootClass.setIdentifier( (KeyValue) getValue() );
			if ( embedded ) {
				rootClass.setEmbeddedIdentifier( true );
			}
			else {
				rootClass.setIdentifierProperty( property );
				final MappedSuperclass superclass =
						getMappedSuperclassOrNull( declaringClass, inheritanceStatePerClass, buildingContext );
				setDeclaredIdentifier( rootClass, superclass, property );
			}
		}
	}

	private void setDeclaredIdentifier(RootClass rootClass, MappedSuperclass superclass, Property prop) {
		handleGenericComponentProperty( prop, memberDetails, buildingContext );
		if ( superclass == null ) {
			rootClass.setDeclaredIdentifierProperty( prop );
		}
		else {
			prepareActualProperty( prop, memberDetails, false, buildingContext,
					superclass::setDeclaredIdentifierProperty );
		}
	}

	private Component getOrCreateCompositeId(RootClass rootClass) {
		final Component id = (Component) rootClass.getIdentifier();
		if ( id == null ) {
			final Component identifier = createEmbeddable(
					holder,
					new PropertyPreloadedData(),
					true,
					false,
					resolveCustomInstantiator( memberDetails, returnedClass.determineRawClass() ),
					buildingContext
			);
			rootClass.setIdentifier( identifier );
			identifier.setNullValueUndefined();
			rootClass.setEmbeddedIdentifier( true );
			rootClass.setIdentifierMapper( identifier );
			return identifier;
		}
		else {
			return id;
		}
	}

	private Class<? extends EmbeddableInstantiator> resolveCustomInstantiator(
			MemberDetails property,
			ClassDetails embeddableClass) {
		final org.hibernate.annotations.EmbeddableInstantiator onEmbedded =
				property.getDirectAnnotationUsage( org.hibernate.annotations.EmbeddableInstantiator.class );
		if ( onEmbedded != null ) {
			return onEmbedded.value();
		}

		final org.hibernate.annotations.EmbeddableInstantiator onEmbeddable =
				embeddableClass.getDirectAnnotationUsage( org.hibernate.annotations.EmbeddableInstantiator.class );
		if ( onEmbeddable != null ) {
			return onEmbeddable.value();
		}

		return null;
	}

	//used when the value is provided and the binding is done elsewhere
	public Property makeProperty() {
		validateMake();
		validateAnnotationsAgainstType();
		Property property = new Property();
		property.setName( name );
		property.setValue( value );
		property.setLazy( lazy );
		property.setLazyGroup( lazyGroup );
		property.setCascade( cascadeTypes );
		property.setPropertyAccessorName( accessType.getType() );
		property.setReturnedClassName( returnedClassName );
//		property.setPropertyAccessStrategy( propertyAccessStrategy );
		handleValueGeneration( property );
		handleNaturalId( property );
		handleLob( property );
		handleMutability( property );
		handleOptional( property );
		inferOptimisticLocking( property );
		return property;
	}

	private void handleValueGeneration(Property property) {
		if ( memberDetails != null ) {
			property.setValueGeneratorCreator(
					createValueGeneratorFromAnnotations( holder, name, value, memberDetails, buildingContext ) );
		}
	}

	private void handleLob(Property property) {
		if ( memberDetails != null ) {
			// HHH-4635 -- needed for dialect-specific property ordering
			property.setLob( memberDetails.hasDirectAnnotationUsage( Lob.class ) );
		}
	}

	private void handleMutability(Property property) {
		if ( memberDetails != null && memberDetails.hasDirectAnnotationUsage( Immutable.class ) ) {
			updatable = false;
		}
		property.setInsertable( insertable );
		property.setUpdateable( updatable );
	}

	private void handleOptional(Property property) {
		if ( memberDetails != null ) {
			property.setOptional( !isId && isOptional( memberDetails, holder ) );
			if ( property.isOptional() ) {
				final OptionalDeterminationSecondPass secondPass = persistentClasses -> {
					// Defer determining whether a property and its columns are nullable,
					// as handleOptional might be called when the value is not yet fully initialized
					if ( property.getPersistentClass() != null ) {
						for ( Join join : property.getPersistentClass().getJoins() ) {
							if ( join.getProperties().contains( property ) ) {
								// If this property is part of a join it is inherently optional
								return;
							}
						}
					}

					if ( !property.getValue().isNullable() ) {
						property.setOptional( false );
					}
				};
				// Always register this as second pass and never execute it directly,
				// even if we are in a second pass already.
				// If we are in a second pass, then we are currently processing the generalSecondPassList
				// to which the following call will add the second pass to,
				// so it will be executed within that second pass, just a bit later
				buildingContext.getMetadataCollector().addSecondPass( secondPass );
			}
		}
	}

	private void handleNaturalId(Property property) {
		if ( memberDetails != null && entityBinder != null ) {
			final NaturalId naturalId = memberDetails.getDirectAnnotationUsage( NaturalId.class );
			if ( naturalId != null ) {
				if ( !entityBinder.isRootEntity() ) {
					throw new AnnotationException( "Property '" + qualify( holder.getPath(), name )
							+ "' belongs to an entity subclass and may not be annotated '@NaturalId'" +
							" (only a property of a root '@Entity' or a '@MappedSuperclass' may be a '@NaturalId')" );
				}
				if ( !naturalId.mutable() ) {
					updatable = false;
				}
				property.setNaturalIdentifier( true );
			}
		}
	}

	private void inferOptimisticLocking(Property property) {
		// this is already handled for collections in CollectionBinder...
		if ( value instanceof org.hibernate.mapping.Collection collection ) {
			property.setOptimisticLocked( collection.isOptimisticLocked() );
		}
		else if ( memberDetails != null && memberDetails.hasDirectAnnotationUsage( OptimisticLock.class ) ) {
			final OptimisticLock optimisticLock = memberDetails.getDirectAnnotationUsage( OptimisticLock.class );
			final boolean excluded = optimisticLock.excluded();
			validateOptimisticLock( excluded );
			property.setOptimisticLocked( !excluded );
		}
		else {
			property.setOptimisticLocked( !isToOneValue(value) || insertable ); // && updatable as well???
		}
	}

	private void validateAnnotationsAgainstType() {
		if ( memberDetails != null ) {
			final TypeDetails type = memberDetails.getType();
			if ( !(type instanceof ArrayTypeDetails) ) {
				checkAnnotation( OrderColumn.class, List.class );
				if ( memberDetails.hasDirectAnnotationUsage( OrderBy.class )
						&& !type.isImplementor( Collection.class )
						&& !type.isImplementor( Map.class ) ) {
					throw new AnnotationException( "Property '" + qualify( holder.getPath(), name )
							+ "' is annotated '@OrderBy' but is not of type 'Collection' or 'Map'" );
				}
			}
			checkAnnotation( MapKey.class, Map.class );
			checkAnnotation( MapKeyColumn.class, Map.class );
			checkAnnotation( MapKeyClass.class, Map.class );
			checkAnnotation( MapKeyEnumerated.class, Map.class );
			checkAnnotation( MapKeyTemporal.class, Map.class );
			checkAnnotation( MapKeyColumn.class, Map.class );
			checkAnnotation( MapKeyJoinColumn.class, Map.class );
			checkAnnotation( MapKeyJoinColumns.class, Map.class );
		}
	}

	private void checkAnnotation(Class<? extends Annotation> annotationClass, Class<?> propertyType) {
		if ( memberDetails.hasDirectAnnotationUsage( annotationClass )
				&& !memberDetails.getType().isImplementor( propertyType ) ) {
			throw new AnnotationException( "Property '" + qualify( holder.getPath(), name )
					+ "' is annotated '@" + annotationClass.getSimpleName()
					+ "' but is not of type '" + propertyType.getTypeName() + "'" );
		}
	}

	private void validateOptimisticLock(boolean excluded) {
		if ( excluded ) {
			if ( memberDetails.hasDirectAnnotationUsage( Version.class ) ) {
				throw new AnnotationException("Property '" + qualify( holder.getPath(), name )
						+ "' is annotated '@OptimisticLock(excluded=true)' and '@Version'" );
			}
			if ( memberDetails.hasDirectAnnotationUsage( Id.class ) ) {
				throw new AnnotationException("Property '" + qualify( holder.getPath(), name )
						+ "' is annotated '@OptimisticLock(excluded=true)' and '@Id'" );
			}
			if ( memberDetails.hasDirectAnnotationUsage( EmbeddedId.class ) ) {
				throw new AnnotationException( "Property '" + qualify( holder.getPath(), name )
						+ "' is annotated '@OptimisticLock(excluded=true)' and '@EmbeddedId'" );
			}
		}
	}

	/**
	 * @param elements List of {@link PropertyData} instances
	 * @param propertyContainer Metadata about a class and its properties
	 * @param idPropertyCounter number of id properties already present in list of {@link PropertyData} instances
	 *
	 * @return total number of id properties found after iterating the elements of {@code annotatedClass}
	 * using the determined access strategy (starting from the provided {@code idPropertyCounter})
	 */
	static int addElementsOfClass(
			List<PropertyData> elements,
			PropertyContainer propertyContainer,
			MetadataBuildingContext context,
			int idPropertyCounter) {
		for ( MemberDetails property : propertyContainer.propertyIterator() ) {
			idPropertyCounter = addProperty( propertyContainer, property, elements, context, idPropertyCounter );
		}
		return idPropertyCounter;
	}

	private static int addProperty(
			PropertyContainer propertyContainer,
			MemberDetails property,
			List<PropertyData> inFlightPropertyDataList,
			MetadataBuildingContext context,
			int idPropertyCounter) {
		final InFlightMetadataCollector collector = context.getMetadataCollector();

		// see if inFlightPropertyDataList already contains a PropertyData for this name,
		// and if so, skip it...
		for ( PropertyData propertyData : inFlightPropertyDataList ) {
			if ( propertyData.getPropertyName().equals( property.resolveAttributeName() ) ) {
				checkIdProperty( property, propertyData, context.getBootstrapContext().getModelsContext() );
				// EARLY EXIT!!!
				return idPropertyCounter;
			}
		}

		final TypeVariableScope ownerType = propertyContainer.getTypeAtStake();

		final PropertyData propertyAnnotatedElement = new PropertyInferredData(
				propertyContainer.getDeclaringClass(),
				ownerType,
				property,
				propertyContainer.getClassLevelAccessType().getType(),
				context
		);

		// put element annotated by @Id in front, since it has to be parsed
		// before any association by Hibernate
		final MemberDetails element = propertyAnnotatedElement.getAttributeMember();
		if ( hasIdAnnotation( element ) ) {
			inFlightPropertyDataList.add( idPropertyCounter, propertyAnnotatedElement );
			if ( hasToOneAnnotation( element ) ) {
				collector.addToOneAndIdProperty( ownerType.determineRawClass(), propertyAnnotatedElement );
			}
			idPropertyCounter++;
		}
		else {
			inFlightPropertyDataList.add( propertyAnnotatedElement );
		}
		if ( element.hasDirectAnnotationUsage( MapsId.class ) ) {
			collector.addPropertyAnnotatedWithMapsId( ownerType.determineRawClass(), propertyAnnotatedElement );
		}

		return idPropertyCounter;
	}

	private static void checkIdProperty(MemberDetails property, PropertyData propertyData, ModelsContext context) {
		final boolean incomingIdProperty = hasIdAnnotation( property );
		final MemberDetails attributeMember = propertyData.getAttributeMember();
		final boolean existingIdProperty = hasIdAnnotation( attributeMember );
		if ( incomingIdProperty ) {
			if ( existingIdProperty ) {
				if ( property.hasDirectAnnotationUsage( GeneratedValue.class )
						|| !property.getMetaAnnotated( IdGeneratorType.class, context ).isEmpty() ) {
					//TODO: it would be nice to allow a root @Entity to override an
					//      @Id field declared by a @MappedSuperclass and change the
					//      generator, but for now we don't seem to be able to detect
					//      that case here
					throw new AnnotationException(
							"Attribute '" + attributeMember.getName()
							+ "' is declared as an '@Id' or '@EmbeddedId' property by '"
							+ attributeMember.getDeclaringType().getName()
							+ "' and so '" + property.getDeclaringType().getName()
							+ "' may not respecify the generation strategy" );
				}
			}
			else {
				//TODO: it would be nice to allow a root @Entity to override a
				//      field declared by a @MappedSuperclass, redeclaring it
				//      as an @Id field, but for now we don't seem to be able
				//      to detect that case here
				throw new AnnotationException(
						"Attribute '" + attributeMember.getName()
						+ "' is declared by '" + attributeMember.getDeclaringType().getName()
						+ "' and may not be redeclared as an '@Id' or '@EmbeddedId' by '"
						+ property.getDeclaringType().getName() + "'" );
			}
		}
	}

	static boolean hasIdAnnotation(MemberDetails element) {
		return element.hasDirectAnnotationUsage( Id.class )
			|| element.hasDirectAnnotationUsage( EmbeddedId.class );
	}

	/**
	 * Process annotation of a particular property or field.
	 */
	public static void processElementAnnotations(
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			boolean isComponentEmbedded,
			boolean inSecondPass,
			MetadataBuildingContext context,
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass) throws MappingException {

		if ( !alreadyProcessedBySuper( propertyHolder, inferredData, entityBinder ) ) {
			// inSecondPass can only be used to apply right away the second pass of a composite-element
			// Because it's a value type, there is no bidirectional association, hence second pass
			// ordering does not matter
			final MemberDetails property = inferredData.getAttributeMember();
			if ( property.hasDirectAnnotationUsage( Parent.class ) ) {
				handleParentProperty( propertyHolder, inferredData, property );
			}
			else {
				//prepare PropertyBinder
				buildProperty(
						propertyHolder,
						nullability,
						inferredData,
						entityBinder,
						isIdentifierMapper,
						isComponentEmbedded,
						inSecondPass,
						context,
						inheritanceStatePerClass,
						property
				);
			}
		}
	}

	private static boolean alreadyProcessedBySuper(PropertyHolder holder, PropertyData data, EntityBinder binder) {
		return !holder.isComponent()
			&& binder.isPropertyDefinedInSuperHierarchy( data.getPropertyName() );
	}

	private static void handleParentProperty(PropertyHolder holder, PropertyData data, MemberDetails property) {
		if ( holder.isComponent() ) {
			holder.setParentProperty( property.resolveAttributeName() );
		}
		else {
			throw new AnnotationException( "Property '" + getPath( holder, data )
					+ "' is annotated '@Parent' but is not a member of an embeddable class" );
		}
	}

	private static void buildProperty(
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			boolean isComponentEmbedded,
			boolean inSecondPass,
			MetadataBuildingContext context,
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass,
			MemberDetails property) {

		if ( isPropertyOfRegularEmbeddable( propertyHolder, isComponentEmbedded )
				&& property.hasDirectAnnotationUsage(Id.class)) {
			throw new AnnotationException("Member '" + property.getName()
					+ "' of embeddable class " + propertyHolder.getClassName() + " is annotated '@Id'");
		}

		final TypeDetails attributeTypeDetails =
				inferredData.getAttributeMember().isPlural()
						? inferredData.getAttributeMember().getType()
						: inferredData.getClassOrElementType();

		final PropertyBinder propertyBinder = propertyBinder(
				propertyHolder,
				inferredData,
				entityBinder,
				isIdentifierMapper,
				context,
				inheritanceStatePerClass,
				property,
				attributeTypeDetails
		);

		final LazyGroup lazyGroupAnnotation = property.getDirectAnnotationUsage( LazyGroup.class );
		if ( lazyGroupAnnotation != null ) {
			propertyBinder.setLazyGroup( lazyGroupAnnotation.value() );
		}

		final ColumnsBuilder columnsBuilder =
				new ColumnsBuilder( propertyHolder, nullability, property, inferredData, entityBinder, context )
						.extractMetadata();
		final AnnotatedJoinColumns joinColumns = columnsBuilder.getJoinColumns();
		final AnnotatedColumns columns = propertyBinder.bindProperty(
				propertyHolder,
				nullability,
				inferredData,
				entityBinder,
				isIdentifierMapper,
				isComponentEmbedded,
				inSecondPass,
				property,
				attributeTypeDetails.determineRawClass(),
				columnsBuilder
		);
		addNaturalIds( inSecondPass, property, columns, joinColumns, context );
	}

	private static PropertyBinder propertyBinder(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			MetadataBuildingContext context,
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass,
			MemberDetails property,
			TypeDetails attributeTypeDetails) {
		final PropertyBinder propertyBinder = new PropertyBinder();
		propertyBinder.setName( inferredData.getPropertyName() );
		propertyBinder.setReturnedClassName( inferredData.getTypeName() );
		propertyBinder.setAccessType( inferredData.getDefaultAccess() );
		propertyBinder.setHolder( propertyHolder );
		propertyBinder.setMemberDetails( property );
		propertyBinder.setReturnedClass( attributeTypeDetails );
		propertyBinder.setBuildingContext( context );
		if ( isIdentifierMapper ) {
			propertyBinder.setInsertable( false );
			propertyBinder.setUpdatable( false );
		}
		propertyBinder.setDeclaringClass( inferredData.getDeclaringClass() );
		propertyBinder.setEntityBinder( entityBinder );
		propertyBinder.setInheritanceStatePerClass( inheritanceStatePerClass );
		propertyBinder.setId( !entityBinder.isIgnoreIdAnnotations() && hasIdAnnotation( property ) );
		return propertyBinder;
	}

	private static boolean isPropertyOfRegularEmbeddable(PropertyHolder propertyHolder, boolean isComponentEmbedded) {
		return propertyHolder.isComponent() // it's a field of some sort of composite value
			&& !propertyHolder.isInIdClass() // it's not a field of an id class
			&& !isComponentEmbedded; // it's not an entity field matching a field of the id class
	}

	private AnnotatedColumns bindProperty(
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			boolean isComponentEmbedded,
			boolean inSecondPass,
			MemberDetails property,
			ClassDetails returnedClass,
			ColumnsBuilder columnsBuilder) {
		if ( isVersion( property ) ) {
			bindVersionProperty(
					propertyHolder,
					inferredData,
					isIdentifierMapper,
					columnsBuilder.getColumns()
			);
		}
		else if ( isManyToOne( property ) ) {
			bindManyToOne(
					propertyHolder,
					inferredData,
					isIdentifierMapper,
					inSecondPass,
					buildingContext,
					property,
					columnsBuilder.getJoinColumns(),
					this
			);
		}
		else if ( isOneToOne( property ) ) {
			bindOneToOne(
					propertyHolder,
					inferredData,
					isIdentifierMapper,
					inSecondPass,
					buildingContext,
					property,
					columnsBuilder.getJoinColumns(),
					this
			);
		}
		else if ( isAny( property ) ) {
			bindAny(
					propertyHolder,
					nullability,
					inferredData,
					entityBinder,
					isIdentifierMapper,
					buildingContext,
					property,
					columnsBuilder.getJoinColumns()
			);
		}
		else if ( isCollection( property ) ) {
			bindCollection(
					propertyHolder,
					nullability,
					inferredData,
					entityBinder,
					isIdentifierMapper,
					buildingContext,
					inheritanceStatePerClass,
					property,
					columnsBuilder.getJoinColumns()
			);
		}
		//Either a regular property or a basic @Id or @EmbeddedId while not ignoring id annotations
		else if ( !isId() || !entityBinder.isIgnoreIdAnnotations() ) {
			// returns overridden columns
			return bindBasicOrComposite(
					propertyHolder,
					nullability,
					inferredData,
					entityBinder,
					isIdentifierMapper,
					isComponentEmbedded,
					property,
					columnsBuilder,
					columnsBuilder.getColumns(),
					returnedClass
			);
		}
		return columnsBuilder.getColumns();
	}

	private static boolean isVersion(MemberDetails property) {
		return property.hasDirectAnnotationUsage( Version.class );
	}

	private static boolean isOneToOne(MemberDetails property) {
		return property.hasDirectAnnotationUsage( OneToOne.class );
	}

	private static boolean isManyToOne(MemberDetails property) {
		return property.hasDirectAnnotationUsage( ManyToOne.class );
	}

	private static boolean isAny(MemberDetails property) {
		return property.hasDirectAnnotationUsage( Any.class );
	}

	private static boolean isCollection(MemberDetails property) {
		return property.hasDirectAnnotationUsage( OneToMany.class )
			|| property.hasDirectAnnotationUsage( ManyToMany.class )
			|| property.hasDirectAnnotationUsage( ElementCollection.class )
			|| property.hasDirectAnnotationUsage( ManyToAny.class );
	}

	private void bindVersionProperty(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			boolean isIdentifierMapper,
			AnnotatedColumns columns) {
		checkVersionProperty( propertyHolder, isIdentifierMapper );
		final RootClass rootClass = (RootClass) propertyHolder.getPersistentClass();
		setColumns( columns );
		final Property property = makePropertyValueAndBind();
		getBasicValueBinder().setVersion( true );
		rootClass.setVersion( property );

		//If version is on a mapped superclass, update the mapping
		final ClassDetails declaringClass = inferredData.getDeclaringClass();
		final MappedSuperclass mappedSuperclass =
				getMappedSuperclassOrNull( declaringClass, inheritanceStatePerClass, buildingContext );
		if ( mappedSuperclass != null ) {
			// Don't overwrite an existing version property
			if ( mappedSuperclass.getDeclaredVersion() == null ) {
				mappedSuperclass.setDeclaredVersion( property );
			}
		}
		else {
			//we know the property is on the actual entity
			rootClass.setDeclaredVersion( property );
		}

		rootClass.setOptimisticLockStyle( OptimisticLockStyle.VERSION );
	}

	private static void checkVersionProperty(PropertyHolder propertyHolder, boolean isIdentifierMapper) {
		if ( isIdentifierMapper ) {
			throw new AnnotationException( "Class '" + propertyHolder.getEntityName()
					+ "' is annotated '@IdClass' and may not have a property annotated '@Version'"
			);
		}
		if ( !( propertyHolder.getPersistentClass() instanceof RootClass ) ) {
			throw new AnnotationException( "Entity '" + propertyHolder.getEntityName()
					+ "' is a subclass in an entity class hierarchy and may not have a property annotated '@Version'" );
		}
		if ( !propertyHolder.isEntity() ) {
			throw new AnnotationException( "Embedded class '" + propertyHolder.getEntityName()
					+ "' may not have a property annotated '@Version'" );
		}
	}

	private AnnotatedColumns bindBasicOrComposite(
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			boolean isComponentEmbedded,
			MemberDetails property,
			ColumnsBuilder columnsBuilder,
			AnnotatedColumns columns,
			ClassDetails returnedClass) {

		// overrides from @MapsId or @IdClass if needed
		final PropertyData overridingProperty =
				overridingProperty( propertyHolder, isIdentifierMapper, property );
		final AnnotatedColumns actualColumns;
		final boolean isComposite;
		if ( overridingProperty != null ) {
			setReferencedEntityName( overridingProperty.getClassOrElementName() );
			actualColumns = columnsBuilder.overrideColumnFromMapperOrMapsIdProperty( overridingProperty );
			isComposite = isComposite( property, returnedClass, overridingProperty );
		}
		else {
			actualColumns = columns;
			isComposite = isEmbedded( property, returnedClass );
		}

		final PropertyBinder propertyBinder = propertyBinder(
				propertyHolder,
				nullability,
				inferredData,
				entityBinder,
				isComposite,
				isIdentifierMapper,
				isComponentEmbedded,
				property,
				columns,
				returnedClass,
				actualColumns,
				overridingProperty
		);
		propertyBinder.handleGenerators(
				propertyHolder,
				inferredData,
				isIdentifierMapper,
				overridingProperty != null,
				overridingProperty
		);
		return actualColumns;
	}

	private PropertyData overridingProperty(
			PropertyHolder propertyHolder,
			boolean isIdentifierMapper,
			MemberDetails property) {
		if ( isIdentifierMapper
			|| isId
			|| propertyHolder.isOrWithinEmbeddedId()
			|| propertyHolder.isInIdClass() ) {
			// the associated entity could be using an @IdClass making the overridden property a component
			return getPropertyOverriddenByMapperOrMapsId(
					isId,
					propertyHolder,
					property.resolveAttributeName(),
					buildingContext
			);
		}
		else {
			return null;
		}
	}

	private PropertyBinder propertyBinder(
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			EntityBinder entityBinder,
			boolean isComposite,
			boolean isIdentifierMapper,
			boolean isComponentEmbedded,
			MemberDetails property,
			AnnotatedColumns columns,
			ClassDetails returnedClass,
			AnnotatedColumns actualColumns,
			PropertyData overridingProperty) {

		final Class<? extends CompositeUserType<?>> compositeUserType =
				resolveCompositeUserType( inferredData, buildingContext );

		if ( isComposite || compositeUserType != null ) {
			if ( property.isArray() && property.getElementType() != null
					&& isEmbedded( property, property.getElementType() ) ) {
				// This is a special kind of basic aggregate component array type
				aggregateBinder(
						propertyHolder,
						inferredData,
						entityBinder,
						isIdentifierMapper,
						isComponentEmbedded,
						property,
						columns,
						returnedClass,
						compositeUserType,
						actualColumns
				);
			}
			else {
				return createCompositeBinder(
						propertyHolder,
						inferredData,
						entityBinder,
						isIdentifierMapper,
						isComponentEmbedded,
						buildingContext,
						inheritanceStatePerClass,
						property,
						actualColumns,
						returnedClass,
						isId(),
						overridingProperty != null,
						overridingProperty,
						compositeUserType
				);
			}
		}
		else if ( property.isPlural() && property.getElementType() != null
					&& isEmbedded( property, property.getElementType() ) ) {
			// This is a special kind of basic aggregate component array type
			aggregateBinder(
					propertyHolder,
					inferredData,
					entityBinder,
					isIdentifierMapper,
					isComponentEmbedded,
					property,
					columns,
					property.getElementType().determineRawClass(),
					null,
					actualColumns
			);
		}
		else {
			basicBinder(
					propertyHolder,
					inferredData,
					nullability,
					property,
					actualColumns
			);
		}
		return this;
	}

	private void handleGenerators(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			boolean isIdentifierMapper,
			boolean isOverridden,
			PropertyData overridingProperty) {
		if ( isOverridden ) {
			handleGeneratorsForOverriddenId( propertyHolder, overridingProperty );
		}
		else if ( isId() ) {
			if ( isIdentifierMapper ) {
				throw new AnnotationException( "Property '"+ getPath( propertyHolder, inferredData )
						+ "' belongs to an '@IdClass' and may not be annotated '@Id' or '@EmbeddedId'" );
			}
			//components and regular basic types create SimpleValue objects
			createIdGeneratorsFromGeneratorAnnotations(
					propertyHolder,
					inferredData,
					(SimpleValue) getValue(),
					buildingContext
			);
		}
	}

	private void aggregateBinder(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			boolean isComponentEmbedded,
			MemberDetails property,
			AnnotatedColumns columns,
			ClassDetails returnedClass,
			Class<? extends CompositeUserType<?>> compositeUserType,
			AnnotatedColumns actualColumns) {
		// This is a special kind of basic aggregate component array type
		setComponentElement(
				bindEmbeddable(
						inferredData,
						propertyHolder,
						entityBinder.getPropertyAccessor( property ),
						entityBinder,
						isIdentifierMapper,
						buildingContext,
						isComponentEmbedded,
						isId(),
						inheritanceStatePerClass,
						determineCustomInstantiator( property, returnedClass, buildingContext ),
						compositeUserType,
						columns
				)
		);
		setColumns( actualColumns );
		makePropertyValueAndBind();
	}

	private boolean isComposite(
			MemberDetails property,
			ClassDetails returnedClass,
			PropertyData overridingProperty) {
		final InheritanceState state =
				inheritanceStatePerClass.get( overridingProperty.getClassOrElementType().determineRawClass() );
		return state != null ? state.hasIdClassOrEmbeddedId() : isEmbedded( property, returnedClass );
	}

	private void handleGeneratorsForOverriddenId(
			PropertyHolder propertyHolder,
			PropertyData mapsIdProperty) {
		final SimpleValue idValue = (SimpleValue) getValue();
		final RootClass rootClass = propertyHolder.getPersistentClass().getRootClass();
		final String propertyName = mapsIdProperty.getPropertyName();
		final String entityName = rootClass.getEntityName();
		idValue.setCustomIdGeneratorCreator( creationContext ->
				new BeforeExecutionGenerator() {
					@Override
					public Object generate(
							SharedSessionContractImplementor session,
							Object owner,
							Object currentValue,
							EventType eventType) {
						return getForeignId( entityName, propertyName, session, owner );
					}
					@Override
					public EnumSet<EventType> getEventTypes() {
						return EventTypeSets.INSERT_ONLY;
					}
					@Override
					public boolean allowAssignedIdentifiers() {
						return true;
					}
				}
		);
	}

	private void basicBinder(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Nullability nullability,
			MemberDetails property,
			AnnotatedColumns columns) {
		if ( shouldForceNotNull( nullability, isExplicitlyOptional( property ) ) ) {
			forceColumnsNotNull( propertyHolder, inferredData, columns );
		}

		setLazy( isLazy( property ) );
		setColumns( columns );
		makePropertyValueAndBind();
	}

	private void forceColumnsNotNull(
			PropertyHolder holder,
			PropertyData data,
			AnnotatedColumns columns) {
		for ( AnnotatedColumn column : columns.getColumns() ) {
			if ( isId() && column.isFormula() ) {
				throw new CannotForceNonNullableException( "Identifier property '"
						+ getPath( holder, data ) + "' cannot map to a '@Formula'" );
			}
			column.forceNotNull();
		}
	}

	private boolean shouldForceNotNull(Nullability nullability, boolean optional) {
		return isId()
			|| !optional && nullability != Nullability.FORCED_NULL;
	}

	/**
	 * Should this property be considered optional, without considering
	 * whether it is primitive?
	 *
	 * @apiNote Poorly named to a degree.
	 *          The intention is really whether non-optional is explicit
	 */
	private static boolean isExplicitlyOptional(MemberDetails attributeMember) {
		final Basic basic = attributeMember.getDirectAnnotationUsage( Basic.class );
		// things are optional (nullable) by default.
		// If there is no annotation, that cannot be altered
		return basic == null || basic.optional();
	}

	/**
	 * Should this property be considered optional, taking into account
	 * whether it is primitive?
	 */
	public static boolean isOptional(MemberDetails attributeMember, PropertyHolder propertyHolder) {
		final Basic basic = attributeMember.getDirectAnnotationUsage( Basic.class );
		if ( basic != null ) {
			return basic.optional()
				&& attributeMember.getType().getTypeKind() != TypeDetails.Kind.PRIMITIVE;
		}
		else if ( attributeMember.isArray() ) {
			return true;
		}
		else if ( propertyHolder != null && propertyHolder.isComponent() ) {
			return true;
		}
		else if ( attributeMember.isPlural() ) {
			return attributeMember.getElementType().getTypeKind() != TypeDetails.Kind.PRIMITIVE;
		}
		else {
			return attributeMember.getType().getTypeKind() != TypeDetails.Kind.PRIMITIVE;
		}
	}

	private static boolean isLazy(MemberDetails property) {
		final Basic annotationUsage = property.getDirectAnnotationUsage( Basic.class );
		return annotationUsage != null && annotationUsage.fetch() == LAZY;
	}

	private static Class<? extends CompositeUserType<?>> resolveCompositeUserType(
			PropertyData inferredData,
			MetadataBuildingContext buildingContext) {
		final ModelsContext modelsContext = buildingContext.getBootstrapContext().getModelsContext();

		final MemberDetails attributeMember = inferredData.getAttributeMember();
		final TypeDetails classOrElementType = inferredData.getClassOrElementType();
		final ClassDetails returnedClass = classOrElementType.determineRawClass();

		if ( attributeMember != null ) {
			final CompositeType compositeType =
					attributeMember.locateAnnotationUsage( CompositeType.class, modelsContext );
			if ( compositeType != null ) {
				return compositeType.value();
			}
			final Class<? extends CompositeUserType<?>> compositeUserType =
					resolveTimeZoneStorageCompositeUserType( attributeMember, returnedClass, buildingContext );
			if ( compositeUserType != null ) {
				return compositeUserType;
			}
		}

		if ( returnedClass != null ) {
			final Class<?> embeddableClass = returnedClass.toJavaClass();
			if ( embeddableClass != null ) {
				return buildingContext.getMetadataCollector().findRegisteredCompositeUserType( embeddableClass );
			}
		}

		return null;
	}

	private static PropertyData getPropertyOverriddenByMapperOrMapsId(
			boolean isId,
			PropertyHolder propertyHolder,
			String propertyName,
			MetadataBuildingContext buildingContext) {
		final ClassDetailsRegistry classDetailsRegistry =
				buildingContext.getBootstrapContext().getModelsContext().getClassDetailsRegistry();
		final PersistentClass persistentClass = propertyHolder.getPersistentClass();
		final String name =
				StringHelper.isEmpty( persistentClass.getClassName() )
						? persistentClass.getEntityName()
						: persistentClass.getClassName();
		final ClassDetails classDetails = classDetailsRegistry.resolveClassDetails( name );
		final InFlightMetadataCollector metadataCollector = buildingContext.getMetadataCollector();
		if ( propertyHolder.isInIdClass() ) {
			final PropertyData data =
					metadataCollector.getPropertyAnnotatedWithIdAndToOne( classDetails, propertyName );
			if ( data != null ) {
				return data;
			}
			// TODO: is this branch even necessary?
			else  {
				return metadataCollector.getPropertyAnnotatedWithMapsId( classDetails, propertyName );
			}
		}
		return metadataCollector.getPropertyAnnotatedWithMapsId( classDetails, isId ? "" : propertyName );
	}

}
