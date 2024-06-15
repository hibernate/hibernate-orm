/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.lang.annotation.Annotation;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AttributeBinderType;
import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.Parent;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.spi.AccessType;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.mapping.Collection;
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
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.models.spi.TypeVariableScope;
import org.hibernate.usertype.CompositeUserType;

import org.jboss.logging.Logger;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;

import static jakarta.persistence.FetchType.LAZY;
import static java.util.Collections.singletonList;
import static org.hibernate.boot.model.internal.AnyBinder.bindAny;
import static org.hibernate.boot.model.internal.BinderHelper.getMappedSuperclassOrNull;
import static org.hibernate.boot.model.internal.BinderHelper.getPath;
import static org.hibernate.boot.model.internal.BinderHelper.getPropertyOverriddenByMapperOrMapsId;
import static org.hibernate.boot.model.internal.BinderHelper.hasToOneAnnotation;
import static org.hibernate.boot.model.internal.ClassPropertyHolder.handleGenericComponentProperty;
import static org.hibernate.boot.model.internal.ClassPropertyHolder.prepareActualProperty;
import static org.hibernate.boot.model.internal.CollectionBinder.bindCollection;
import static org.hibernate.boot.model.internal.EmbeddableBinder.createCompositeBinder;
import static org.hibernate.boot.model.internal.EmbeddableBinder.createEmbeddable;
import static org.hibernate.boot.model.internal.EmbeddableBinder.isEmbedded;
import static org.hibernate.boot.model.internal.GeneratorBinder.createIdGeneratorsFromGeneratorAnnotations;
import static org.hibernate.boot.model.internal.GeneratorBinder.createValueGeneratorFromAnnotations;
import static org.hibernate.boot.model.internal.TimeZoneStorageHelper.resolveTimeZoneStorageCompositeUserType;
import static org.hibernate.boot.model.internal.ToOneBinder.bindManyToOne;
import static org.hibernate.boot.model.internal.ToOneBinder.bindOneToOne;
import static org.hibernate.boot.model.naming.Identifier.toIdentifier;
import static org.hibernate.id.IdentifierGeneratorHelper.getForeignId;
import static org.hibernate.internal.util.StringHelper.qualify;

/**
 * A stateful binder responsible for creating {@link Property} objects.
 *
 * @author Emmanuel Bernard
 */
public class PropertyBinder {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger( CoreMessageLogger.class, PropertyBinder.class.getName() );

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
	private String cascade;
	private BasicValueBinder basicValueBinder;
	private ClassDetails declaringClass;
	private boolean declaringClassSet;
	private boolean embedded;
	private EntityBinder entityBinder;
	private boolean toMany;
	private String referencedEntityName;

	protected SourceModelBuildingContext getSourceModelContext() {
		return buildingContext.getMetadataCollector().getSourceModelBuildingContext();
	}

	public void setReferencedEntityName(String referencedEntityName) {
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

	public void setReturnedClassName(String returnedClassName) {
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

	public void setComponentElement(Component componentElement) {
		this.componentElement = componentElement;
	}

	public void setCascade(String cascadeStrategy) {
		this.cascade = cascadeStrategy;
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

	public void setReturnedClass(TypeDetails returnedClass) {
		this.returnedClass = returnedClass;
	}

	public BasicValueBinder getBasicValueBinder() {
		return basicValueBinder;
	}

	public Value getValue() {
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

		LOG.debugf( "MetadataSourceProcessor property %s with lazy=%s", name, lazy );
		final String containerClassName = holder.getClassName();
		holder.startingProperty( memberDetails );

		basicValueBinder = new BasicValueBinder( BasicValueBinder.Kind.ATTRIBUTE, buildingContext );
		basicValueBinder.setPropertyName( name );
		basicValueBinder.setReturnedClassName( returnedClassName );
		basicValueBinder.setColumns( columns );
		basicValueBinder.setPersistentClassName( containerClassName );
		basicValueBinder.setType(
				memberDetails,
				returnedClass,
				containerClassName,
				holder.resolveAttributeConverterDescriptor( memberDetails )
		);
		basicValueBinder.setReferencedEntityName( referencedEntityName );
		basicValueBinder.setAccessType( accessType );

		value = basicValueBinder.make();

		return makeProperty();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void callAttributeBinders(Property property, Map<String, PersistentClass> persistentClasses) {
		final List<? extends Annotation> metaAnnotatedTargets = memberDetails.getMetaAnnotated(
				AttributeBinderType.class,
				getSourceModelContext()
		);

		if ( CollectionHelper.isEmpty( metaAnnotatedTargets ) ) {
			return;
		}

		final AnnotationDescriptorRegistry descriptorRegistry = getSourceModelContext().getAnnotationDescriptorRegistry();
		for ( int i = 0; i < metaAnnotatedTargets.size(); i++ ) {
			final Annotation metaAnnotatedTarget = metaAnnotatedTargets.get( i );
			final AnnotationDescriptor<? extends Annotation> metaAnnotatedDescriptor = descriptorRegistry.getDescriptor( metaAnnotatedTarget.annotationType() );
			final AttributeBinderType binderTypeAnn = metaAnnotatedDescriptor.getDirectAnnotationUsage( AttributeBinderType.class );
			try {
				final AttributeBinder binder = binderTypeAnn.binder().getConstructor().newInstance();
				final PersistentClass persistentClass = entityBinder != null
						? entityBinder.getPersistentClass()
						: persistentClasses.get( holder.getEntityName() );
				binder.bind( metaAnnotatedTarget, buildingContext, persistentClass, property );
			}
			catch ( Exception e ) {
				throw new AnnotationException( "error processing @AttributeBinderType annotation '" + metaAnnotatedDescriptor.getAnnotationType().getName() + "'", e );
			}
		}
	}

	//used when value is provided
	public Property makePropertyAndBind() {
		return bind( makeProperty() );
	}

	//used to build everything from scratch
	public Property makePropertyValueAndBind() {
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

	private void callAttributeBindersInSecondPass(Property property) {
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
				rootClass.setIdentifierProperty(property);
				final MappedSuperclass superclass =
						getMappedSuperclassOrNull( declaringClass, inheritanceStatePerClass, buildingContext );
				setDeclaredIdentifier( rootClass, superclass, property);
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
			identifier.setNullValue( "undefined" );
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
		final org.hibernate.annotations.EmbeddableInstantiator onEmbedded = property.getDirectAnnotationUsage( org.hibernate.annotations.EmbeddableInstantiator.class );
		if ( onEmbedded != null ) {
			return onEmbedded.value();
		}

		final org.hibernate.annotations.EmbeddableInstantiator onEmbeddable = embeddableClass.getDirectAnnotationUsage( org.hibernate.annotations.EmbeddableInstantiator.class );
		if ( onEmbeddable != null ) {
			return onEmbeddable.value();
		}

		return null;
	}

	//used when the value is provided and the binding is done elsewhere
	public Property makeProperty() {
		validateMake();
		LOG.debugf( "Building property %s", name );
		Property property = new Property();
		property.setName( name );
		property.setValue( value );
		property.setLazy( lazy );
		property.setLazyGroup( lazyGroup );
		property.setCascade( cascade );
		property.setPropertyAccessorName( accessType.getType() );
		property.setReturnedClassName( returnedClassName );
//		property.setPropertyAccessStrategy( propertyAccessStrategy );
		handleValueGeneration( property );
		handleNaturalId( property );
		handleLob( property );
		handleMutability( property );
		handleOptional( property );
		inferOptimisticLocking( property );
		LOG.tracev( "Cascading {0} with {1}", name, cascade );
		callAttributeBindersInSecondPass( property );
		return property;
	}

	private void handleValueGeneration(Property property) {
		if ( memberDetails != null ) {
			property.setValueGeneratorCreator(
					createValueGeneratorFromAnnotations( holder, name, memberDetails, buildingContext ) );
		}
	}

	private void handleLob(Property property) {
		if ( this.memberDetails != null ) {
			// HHH-4635 -- needed for dialect-specific property ordering
			property.setLob( this.memberDetails.hasDirectAnnotationUsage( Lob.class ) );
		}
	}

	private void handleMutability(Property property) {
		if ( this.memberDetails != null && this.memberDetails.hasDirectAnnotationUsage( Immutable.class ) ) {
			updatable = false;
		}
		property.setInsertable( insertable );
		property.setUpdateable( updatable );
	}

	private void handleOptional(Property property) {
		if ( this.memberDetails != null ) {
			property.setOptional( !isId && isOptional( this.memberDetails, this.holder ) );
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
		if ( this.memberDetails != null && entityBinder != null ) {
			final NaturalId naturalId = this.memberDetails.getDirectAnnotationUsage( NaturalId.class );
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
		if ( value instanceof Collection ) {
			property.setOptimisticLocked( ((Collection) value).isOptimisticLocked() );
		}
		else if ( this.memberDetails != null && this.memberDetails.hasDirectAnnotationUsage( OptimisticLock.class ) ) {
			final OptimisticLock optimisticLock = this.memberDetails.getDirectAnnotationUsage( OptimisticLock.class );
			final boolean excluded = optimisticLock.excluded();
			validateOptimisticLock( excluded );
			property.setOptimisticLocked( !excluded );
		}
		else {
			property.setOptimisticLocked( !isToOneValue(value) || insertable ); // && updatable as well???
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
	 *
	 * @return the number of id properties found while iterating the elements of
	 *         {@code annotatedClass} using the determined access strategy
	 */
	static int addElementsOfClass(
			List<PropertyData> elements,
			PropertyContainer propertyContainer,
			MetadataBuildingContext context) {
		int idPropertyCounter = 0;
		for ( MemberDetails property : propertyContainer.propertyIterator() ) {
			idPropertyCounter += addProperty( propertyContainer, property, elements, context );
		}
		return idPropertyCounter;
	}

	private static int addProperty(
			PropertyContainer propertyContainer,
			MemberDetails property,
			List<PropertyData> inFlightPropertyDataList,
			MetadataBuildingContext context) {
		// see if inFlightPropertyDataList already contains a PropertyData for this name,
		// and if so, skip it...
		for ( PropertyData propertyData : inFlightPropertyDataList ) {
			if ( propertyData.getPropertyName().equals( property.resolveAttributeName() ) ) {
				checkIdProperty( property, propertyData );
				// EARLY EXIT!!!
				return 0;
			}
		}

		final ClassDetails declaringClass = propertyContainer.getDeclaringClass();
		final TypeVariableScope ownerType = propertyContainer.getTypeAtStake();
		int idPropertyCounter = 0;
		final PropertyData propertyAnnotatedElement = new PropertyInferredData(
				declaringClass,
				ownerType,
				property,
				propertyContainer.getClassLevelAccessType().getType(),
				context
		);

		// put element annotated by @Id in front, since it has to be parsed
		// before any association by Hibernate
		final MemberDetails element = propertyAnnotatedElement.getAttributeMember();
		if ( hasIdAnnotation( element ) ) {
			inFlightPropertyDataList.add( 0, propertyAnnotatedElement );
			handleIdProperty( propertyContainer, context, declaringClass, ownerType, element );
			if ( hasToOneAnnotation( element ) ) {
				context.getMetadataCollector().addToOneAndIdProperty( ownerType.determineRawClass(), propertyAnnotatedElement );
			}
			idPropertyCounter++;
		}
		else {
			inFlightPropertyDataList.add( propertyAnnotatedElement );
		}
		if ( element.hasDirectAnnotationUsage( MapsId.class ) ) {
			context.getMetadataCollector().addPropertyAnnotatedWithMapsId( ownerType.determineRawClass(), propertyAnnotatedElement );
		}

		return idPropertyCounter;
	}

	private static void checkIdProperty(MemberDetails property, PropertyData propertyData) {
		final Id incomingIdProperty = property.getDirectAnnotationUsage( Id.class );
		final Id existingIdProperty = propertyData.getAttributeMember().getDirectAnnotationUsage( Id.class );
		if ( incomingIdProperty != null && existingIdProperty == null ) {
			throw new MappingException(
					String.format(
							"You cannot override the [%s] non-identifier property from the [%s] base class or @MappedSuperclass and make it an identifier in the [%s] subclass",
							propertyData.getAttributeMember().getName(),
							propertyData.getAttributeMember().getDeclaringType().getName(),
							property.getDeclaringType().getName()
					)
			);
		}
	}

	private static void handleIdProperty(
			PropertyContainer propertyContainer,
			MetadataBuildingContext context,
			ClassDetails declaringClass,
			TypeVariableScope ownerType,
			MemberDetails element) {
		// The property must be put in hibernate.properties as it's a system wide property. Fixable?
		//TODO support true/false/default on the property instead of present / not present
		//TODO is @Column mandatory?
		//TODO add method support
		final SourceModelBuildingContext sourceModelContext = context.getMetadataCollector().getSourceModelBuildingContext();
		if ( context.getBuildingOptions().isSpecjProprietarySyntaxEnabled() ) {
			if ( element.hasDirectAnnotationUsage( Id.class ) && element.hasDirectAnnotationUsage( Column.class ) ) {
				final String columnName = element.getDirectAnnotationUsage( Column.class ).name();
				declaringClass.forEachField( (index, fieldDetails) -> {
					if ( !element.hasDirectAnnotationUsage( MapsId.class ) && isJoinColumnPresent( columnName, element, sourceModelContext ) ) {
						//create a PropertyData for the specJ property holding the mapping
						context.getMetadataCollector().addPropertyAnnotatedWithMapsIdSpecj(
								ownerType.determineRawClass(),
								new PropertyInferredData(
										declaringClass,
										ownerType,
										//same dec
										element,
										// the actual @XToOne property
										propertyContainer.getClassLevelAccessType().getType(),
										//TODO we should get the right accessor but the same as id would do
										context
								),
								element.toString()
						);
					}
				} );
			}
		}
	}

	private static boolean isJoinColumnPresent(String columnName, MemberDetails property, SourceModelBuildingContext modelContext) {
		//The detection of a configured individual JoinColumn differs between Annotation
		//and XML configuration processing.
		final JoinColumn[] joinColumnAnnotations = property.getRepeatedAnnotationUsages(
				JpaAnnotations.JOIN_COLUMN,
				modelContext
		);
		for ( JoinColumn joinColumnAnnotation : joinColumnAnnotations ) {
			if ( joinColumnAnnotation.name().equals( columnName ) ) {
				return true;
			}
		}
		return false;
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
			Map<String, IdentifierGeneratorDefinition> classGenerators,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			boolean isComponentEmbedded,
			boolean inSecondPass,
			MetadataBuildingContext context,
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass) throws MappingException {

		if ( alreadyProcessedBySuper( propertyHolder, inferredData, entityBinder ) ) {
			LOG.debugf(
					"Skipping attribute [%s : %s] as it was already processed as part of super hierarchy",
					inferredData.getClassOrElementName(),
					inferredData.getPropertyName()
			);
		}
		else {
			// inSecondPass can only be used to apply right away the second pass of a composite-element
			// Because it's a value type, there is no bidirectional association, hence second pass
			// ordering does not matter

			if ( LOG.isTraceEnabled() ) {
				LOG.tracev(
						"Processing annotations of {0}.{1}" ,
						propertyHolder.getEntityName(),
						inferredData.getPropertyName()
				);
			}

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
						classGenerators,
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
			Map<String, IdentifierGeneratorDefinition> classGenerators,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			boolean isComponentEmbedded,
			boolean inSecondPass,
			MetadataBuildingContext context,
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass,
			MemberDetails property) {
		final TypeDetails attributeTypeDetails = inferredData.getAttributeMember().isPlural()
				? inferredData.getAttributeMember().getType()
				: inferredData.getClassOrElementType();
		final ClassDetails attributeClassDetails = attributeTypeDetails.determineRawClass();
		final ColumnsBuilder columnsBuilder = new ColumnsBuilder(
				propertyHolder,
				nullability,
				property,
				inferredData,
				entityBinder,
				context
		).extractMetadata();

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

		final LazyGroup lazyGroupAnnotation = property.getDirectAnnotationUsage( LazyGroup.class );
		if ( lazyGroupAnnotation != null ) {
			propertyBinder.setLazyGroup( lazyGroupAnnotation.value() );
		}

		final AnnotatedJoinColumns joinColumns = columnsBuilder.getJoinColumns();
		final AnnotatedColumns columns = bindProperty(
				propertyHolder,
				nullability,
				inferredData,
				classGenerators,
				entityBinder,
				isIdentifierMapper,
				isComponentEmbedded,
				inSecondPass,
				context,
				inheritanceStatePerClass,
				property,
				attributeClassDetails,
				columnsBuilder,
				propertyBinder
		);
		addNaturalIds( inSecondPass, property, columns, joinColumns, context );
	}

	private static AnnotatedColumns bindProperty(
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			Map<String, IdentifierGeneratorDefinition> classGenerators,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			boolean isComponentEmbedded,
			boolean inSecondPass,
			MetadataBuildingContext context,
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass,
			MemberDetails property,
			ClassDetails returnedClass,
			ColumnsBuilder columnsBuilder,
			PropertyBinder propertyBinder) {
		if ( isVersion( property ) ) {
			bindVersionProperty(
					propertyHolder,
					inferredData,
					isIdentifierMapper,
					context,
					inheritanceStatePerClass,
					columnsBuilder.getColumns(),
					propertyBinder
			);
		}
		else if ( isManyToOne( property ) ) {
			bindManyToOne(
					propertyHolder,
					inferredData,
					isIdentifierMapper,
					inSecondPass,
					context,
					property,
					columnsBuilder.getJoinColumns(),
					propertyBinder
			);
		}
		else if ( isOneToOne( property ) ) {
			bindOneToOne(
					propertyHolder,
					inferredData,
					isIdentifierMapper,
					inSecondPass,
					context,
					property,
					columnsBuilder.getJoinColumns(),
					propertyBinder
			);
		}
		else if ( isAny( property ) ) {
			bindAny(
					propertyHolder,
					nullability,
					inferredData,
					entityBinder,
					isIdentifierMapper,
					context,
					property,
					columnsBuilder.getJoinColumns()
			);
		}
		else if ( isCollection( property ) ) {
			bindCollection(
					propertyHolder,
					nullability,
					inferredData,
					classGenerators,
					entityBinder,
					isIdentifierMapper,
					context,
					inheritanceStatePerClass,
					property,
					columnsBuilder.getJoinColumns()
			);
		}
		//Either a regular property or a basic @Id or @EmbeddedId while not ignoring id annotations
		else if ( !propertyBinder.isId() || !entityBinder.isIgnoreIdAnnotations() ) {
			// returns overridden columns
			return bindBasic(
					propertyHolder,
					nullability,
					inferredData,
					classGenerators,
					entityBinder,
					isIdentifierMapper,
					isComponentEmbedded,
					context,
					inheritanceStatePerClass,
					property,
					columnsBuilder,
					columnsBuilder.getColumns(),
					returnedClass,
					propertyBinder
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

	private static boolean isForcePersist(MemberDetails property) {
		return property.hasDirectAnnotationUsage( MapsId.class )
			|| property.hasDirectAnnotationUsage( Id.class );
	}

	private static void bindVersionProperty(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			boolean isIdentifierMapper,
			MetadataBuildingContext context,
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass,
			AnnotatedColumns columns,
			PropertyBinder propertyBinder) {
		checkVersionProperty( propertyHolder, isIdentifierMapper );
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev( "{0} is a version property", inferredData.getPropertyName() );
		}
		final RootClass rootClass = (RootClass) propertyHolder.getPersistentClass();
		propertyBinder.setColumns( columns );
		final Property property = propertyBinder.makePropertyValueAndBind();
		propertyBinder.getBasicValueBinder().setVersion( true );
		rootClass.setVersion( property );

		//If version is on a mapped superclass, update the mapping
		final ClassDetails declaringClass = inferredData.getDeclaringClass();
		final org.hibernate.mapping.MappedSuperclass superclass =
				getMappedSuperclassOrNull( declaringClass, inheritanceStatePerClass, context );
		if ( superclass != null ) {
			superclass.setDeclaredVersion( property );
		}
		else {
			//we know the property is on the actual entity
			rootClass.setDeclaredVersion( property );
		}

		rootClass.setOptimisticLockStyle( OptimisticLockStyle.VERSION );
		if ( LOG.isTraceEnabled() ) {
			final SimpleValue versionValue = (SimpleValue) rootClass.getVersion().getValue();
			LOG.tracev( "Version name: {0}, unsavedValue: {1}",
					rootClass.getVersion().getName(),
					versionValue.getNullValue() );
		}
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

	private static AnnotatedColumns bindBasic(
			PropertyHolder propertyHolder,
			Nullability nullability,
			PropertyData inferredData,
			Map<String, IdentifierGeneratorDefinition> classGenerators,
			EntityBinder entityBinder,
			boolean isIdentifierMapper,
			boolean isComponentEmbedded,
			MetadataBuildingContext context,
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass,
			MemberDetails property,
			ColumnsBuilder columnsBuilder,
			AnnotatedColumns columns,
			ClassDetails returnedClass,
			PropertyBinder propertyBinder) {

		// overrides from @MapsId or @IdClass if needed
		final boolean isComposite;
		final boolean isOverridden;
		final AnnotatedColumns actualColumns;
		if ( isIdentifierMapper
				|| propertyBinder.isId()
				|| propertyHolder.isOrWithinEmbeddedId()
				|| propertyHolder.isInIdClass() ) {
			// the associated entity could be using an @IdClass making the overridden property a component
			final PropertyData overridingProperty = getPropertyOverriddenByMapperOrMapsId(
					propertyBinder.isId(),
					propertyHolder,
					property.resolveAttributeName(),
					context
			);
			if ( overridingProperty != null ) {
				isOverridden = true;
				isComposite = isComposite( inheritanceStatePerClass, property, returnedClass, overridingProperty );
				//Get the new column
				actualColumns = columnsBuilder.overrideColumnFromMapperOrMapsIdProperty( propertyBinder.isId() );
			}
			else {
				isOverridden = false;
				isComposite = isEmbedded( property, returnedClass );
				actualColumns = columns;
			}
		}
		else {
			isOverridden = false;
			isComposite = isEmbedded( property, returnedClass );
			actualColumns = columns;
		}

		final Class<? extends CompositeUserType<?>> compositeUserType =
				resolveCompositeUserType( inferredData, context );

		if ( isComposite || compositeUserType != null ) {
			if ( property.isArray() && property.getElementType() != null
					&& isEmbedded( property, property.getElementType() ) ) {
				// This is a special kind of basic aggregate component array type
				propertyBinder.setComponentElement(
						EmbeddableBinder.bindEmbeddable(
								inferredData,
								propertyHolder,
								entityBinder.getPropertyAccessor( property ),
								entityBinder,
								isIdentifierMapper,
								context,
								isComponentEmbedded,
								propertyBinder.isId(),
								inheritanceStatePerClass,
								null,
								null,
								EmbeddableBinder.determineCustomInstantiator( property, returnedClass, context ),
								compositeUserType,
								null,
								columns
						)
				);
				propertyBinder.setColumns( actualColumns );
				propertyBinder.makePropertyValueAndBind();
			}
			else {
				propertyBinder = createCompositeBinder(
						propertyHolder,
						inferredData,
						entityBinder,
						isIdentifierMapper,
						isComponentEmbedded,
						context,
						inheritanceStatePerClass,
						property,
						actualColumns,
						returnedClass,
						propertyBinder,
						isOverridden,
						compositeUserType
				);
			}
		}
		else if ( property.isPlural()
				&& property.getElementType() != null
				&& isEmbedded( property, property.getElementType() ) ) {
			// This is a special kind of basic aggregate component array type
			propertyBinder.setComponentElement(
					EmbeddableBinder.bindEmbeddable(
							inferredData,
							propertyHolder,
							entityBinder.getPropertyAccessor( property ),
							entityBinder,
							isIdentifierMapper,
							context,
							isComponentEmbedded,
							propertyBinder.isId(),
							inheritanceStatePerClass,
							null,
							null,
							EmbeddableBinder.determineCustomInstantiator( property, property.getElementType().determineRawClass(), context ),
							compositeUserType,
							null,
							columns
					)
			);
			propertyBinder.setColumns( actualColumns );
			propertyBinder.makePropertyValueAndBind();
		}
		else {
			createBasicBinder(
					propertyHolder,
					inferredData,
					nullability,
					context,
					property,
					actualColumns,
					propertyBinder,
					isOverridden
			);
		}
		if ( isOverridden ) {
			handleGeneratorsForOverriddenId(
					propertyHolder,
//					classGenerators,
					context,
					property,
					propertyBinder
			);
		}
		else if ( propertyBinder.isId() ) {
			//components and regular basic types create SimpleValue objects
			if ( isIdentifierMapper ) {
				throw new AnnotationException( "Property '"+ getPath( propertyHolder, inferredData )
						+ "' belongs to an '@IdClass' and may not be annotated '@Id' or '@EmbeddedId'" );
			}
			createIdGeneratorsFromGeneratorAnnotations(
					propertyHolder,
					inferredData,
					(SimpleValue) propertyBinder.getValue(),
					classGenerators,
					context
			);
		}
		return actualColumns;
	}

	private static boolean isComposite(
			Map<ClassDetails, InheritanceState> inheritanceStatePerClass,
			MemberDetails property,
			ClassDetails returnedClass,
			PropertyData overridingProperty) {
		final InheritanceState state = inheritanceStatePerClass.get( overridingProperty.getClassOrElementType().determineRawClass() );
		return state != null ? state.hasIdClassOrEmbeddedId() : isEmbedded( property, returnedClass );
	}

	private static void handleGeneratorsForOverriddenId(
			PropertyHolder propertyHolder,
//			Map<String, IdentifierGeneratorDefinition> classGenerators,
			MetadataBuildingContext context,
			MemberDetails property,
			PropertyBinder propertyBinder) {
		final PropertyData mapsIdProperty = getPropertyOverriddenByMapperOrMapsId(
				propertyBinder.isId(),
				propertyHolder,
				property.resolveAttributeName(),
				context
		);
		final SimpleValue idValue = (SimpleValue) propertyBinder.getValue();
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

	private static void createBasicBinder(
			PropertyHolder propertyHolder,
			PropertyData inferredData,
			Nullability nullability,
			MetadataBuildingContext context,
			MemberDetails property,
			AnnotatedColumns columns,
			PropertyBinder propertyBinder,
			boolean isOverridden) {
		if ( shouldForceNotNull( nullability, propertyBinder, isExplicitlyOptional( property ) ) ) {
			forceColumnsNotNull( propertyHolder, inferredData, columns, propertyBinder );
		}

		propertyBinder.setLazy( isLazy( property ) );
		propertyBinder.setColumns( columns );
		if ( isOverridden ) {
			final PropertyData mapsIdProperty = getPropertyOverriddenByMapperOrMapsId(
					propertyBinder.isId(),
					propertyHolder,
					property.resolveAttributeName(),
					context
			);
			propertyBinder.setReferencedEntityName( mapsIdProperty.getClassOrElementName() );
		}

		propertyBinder.makePropertyValueAndBind();
	}

	private static void forceColumnsNotNull(
			PropertyHolder holder,
			PropertyData data,
			AnnotatedColumns columns,
			PropertyBinder binder) {
		for ( AnnotatedColumn column : columns.getColumns() ) {
			if ( binder.isId() && column.isFormula() ) {
				throw new CannotForceNonNullableException( "Identifier property '"
						+ getPath( holder, data ) + "' cannot map to a '@Formula'" );
			}
			column.forceNotNull();
		}
	}

	private static boolean shouldForceNotNull(Nullability nullability, PropertyBinder binder, boolean optional) {
		return binder.isId()
			|| !optional && nullability != Nullability.FORCED_NULL;
	}

	/**
	 * Should this property be considered optional, without considering
	 * whether it is primitive?
	 *
	 * @apiNote Poorly named to a degree.  The intention is really whether non-optional is explicit
	 */
	private static boolean isExplicitlyOptional(MemberDetails attributeMember) {
		final Basic basicAnn = attributeMember.getDirectAnnotationUsage( Basic.class );
		if ( basicAnn == null ) {
			// things are optional (nullable) by default.  If there is no annotation, that cannot be altered
			return true;
		}

		return basicAnn.optional();
	}

	/**
	 * Should this property be considered optional, taking into
	 * account whether it is primitive?
	 */
	public static boolean isOptional(MemberDetails attributeMember, PropertyHolder propertyHolder) {
		final Basic basicAnn = attributeMember.getDirectAnnotationUsage( Basic.class );
		if ( basicAnn != null ) {
			return basicAnn.optional();
		}

		if ( attributeMember.isArray() ) {
			return true;
		}

		if ( propertyHolder != null && propertyHolder.isComponent() ) {
			return true;
		}

		if ( attributeMember.isPlural() ) {
			return attributeMember.getElementType().getTypeKind() != TypeDetails.Kind.PRIMITIVE;
		}

		return attributeMember.getType().getTypeKind() != TypeDetails.Kind.PRIMITIVE;
	}

	private static boolean isLazy(MemberDetails property) {
		final Basic annotationUsage = property.getDirectAnnotationUsage( Basic.class );
		return annotationUsage != null && annotationUsage.fetch() == LAZY;
	}

	private static void addNaturalIds(
			boolean inSecondPass,
			MemberDetails property,
			AnnotatedColumns columns,
			AnnotatedJoinColumns joinColumns,
			MetadataBuildingContext context) {
		// Natural ID columns must reside in one single UniqueKey within the Table.
		// For now, simply ensure consistent naming.
		// TODO: AFAIK, there really isn't a reason for these UKs to be created
		//       on the SecondPass. This whole area should go away...
		final NaturalId naturalId = property.getDirectAnnotationUsage( NaturalId.class );
		if ( naturalId != null ) {
			final Database database = context.getMetadataCollector().getDatabase();
			final ImplicitNamingStrategy implicitNamingStrategy = context.getBuildingOptions().getImplicitNamingStrategy();
			if ( joinColumns != null ) {
				final Identifier name = implicitNamingStrategy.determineUniqueKeyName( new ImplicitUniqueKeyNameSource() {
					@Override
					public Identifier getTableName() {
						return joinColumns.getTable().getNameIdentifier();
					}

					@Override
					public List<Identifier> getColumnNames() {
						return singletonList(toIdentifier("_NaturalID"));
					}

					@Override
					public Identifier getUserProvidedIdentifier() {
						return null;
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						return context;
					}
				});
				final String keyName = name.render( database.getDialect() );
				for ( AnnotatedColumn column : joinColumns.getColumns() ) {
					column.addUniqueKey( keyName, inSecondPass );
				}
			}
			else {
				final Identifier name = implicitNamingStrategy.determineUniqueKeyName(new ImplicitUniqueKeyNameSource() {
					@Override
					public Identifier getTableName() {
						return columns.getTable().getNameIdentifier();
					}

					@Override
					public List<Identifier> getColumnNames() {
						return singletonList(toIdentifier("_NaturalID"));
					}

					@Override
					public Identifier getUserProvidedIdentifier() {
						return null;
					}

					@Override
					public MetadataBuildingContext getBuildingContext() {
						return context;
					}
				});
				final String keyName = name.render( database.getDialect() );
				for ( AnnotatedColumn column : columns.getColumns() ) {
					column.addUniqueKey( keyName, inSecondPass );
				}
			}
		}
	}

	private static Class<? extends CompositeUserType<?>> resolveCompositeUserType(
			PropertyData inferredData,
			MetadataBuildingContext context) {
		final SourceModelBuildingContext sourceModelContext = context.getMetadataCollector().getSourceModelBuildingContext();
		final MemberDetails attributeMember = inferredData.getAttributeMember();
		final TypeDetails classOrElementType = inferredData.getClassOrElementType();
		final ClassDetails returnedClass = classOrElementType.determineRawClass();

		if ( attributeMember != null ) {
			final CompositeType compositeType = attributeMember.locateAnnotationUsage( CompositeType.class, sourceModelContext );
			if ( compositeType != null ) {
				return compositeType.value();
			}
			final Class<? extends CompositeUserType<?>> compositeUserType = resolveTimeZoneStorageCompositeUserType( attributeMember, returnedClass, context );
			if ( compositeUserType != null ) {
				return compositeUserType;
			}
		}

		if ( returnedClass != null ) {
			final Class<?> embeddableClass = returnedClass.toJavaClass();
			if ( embeddableClass != null ) {
				return context.getMetadataCollector().findRegisteredCompositeUserType( embeddableClass );
			}
		}

		return null;
	}
}
