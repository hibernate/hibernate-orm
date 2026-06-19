/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.persistence.FetchType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedNativeStatement;
import jakarta.persistence.NamedStatement;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.MappingException;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.SimpleAuxiliaryDatabaseObject;
import org.hibernate.boot.model.internal.GeneratorParameters;
import org.hibernate.boot.model.internal.QueryBinder;
import org.hibernate.boot.models.AnnotationPlacementException;
import org.hibernate.boot.mapping.ModelBindingLogging;
import org.hibernate.boot.mapping.internal.model.EntityHierarchyBinding;
import org.hibernate.boot.mapping.internal.model.EntityTypeBinding;
import org.hibernate.boot.mapping.internal.model.EmbeddableTypeBinding;
import org.hibernate.boot.mapping.internal.model.ManagedTypeBinding;
import org.hibernate.boot.mapping.internal.model.MappedSuperclassTypeBinding;
import org.hibernate.boot.mapping.internal.categorize.AttributeMetadata;
import org.hibernate.boot.mapping.internal.categorize.CategorizedDomainModel;
import org.hibernate.boot.mapping.internal.categorize.CollectionTypeRegistration;
import org.hibernate.boot.mapping.internal.categorize.CompositeUserTypeRegistration;
import org.hibernate.boot.mapping.internal.categorize.ConversionRegistration;
import org.hibernate.boot.mapping.internal.categorize.DatabaseObjectRegistration;
import org.hibernate.boot.mapping.internal.categorize.EmbeddableInstantiatorRegistration;
import org.hibernate.boot.mapping.internal.categorize.EntityHierarchy;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.FetchProfileRegistration;
import org.hibernate.boot.mapping.internal.categorize.GenericGeneratorRegistration;
import org.hibernate.boot.mapping.internal.categorize.GlobalRegistrations;
import org.hibernate.boot.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.JavaTypeRegistration;
import org.hibernate.boot.mapping.internal.categorize.JdbcTypeRegistration;
import org.hibernate.boot.mapping.internal.categorize.ManagedTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.MappedSuperclassTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.SequenceGeneratorRegistration;
import org.hibernate.boot.mapping.internal.categorize.TableGeneratorRegistration;
import org.hibernate.boot.mapping.internal.categorize.UserTypeRegistration;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingOptions;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.MetadataSource;
import org.hibernate.mapping.RootClass;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.ModelsException;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserCollectionType;
import org.hibernate.usertype.UserType;

import jakarta.persistence.AttributeConverter;

import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/// Coordinates binding of a categorized domain model into Hibernate's boot-time
/// mapping model.
///
/// The coordinator is the entry point for the binding phase.  It applies global
/// registrations, visits each categorized entity hierarchy, creates the appropriate
/// type binders, and then runs ordered binding phases that make type, table,
/// identifier, member, and association state available to later phases.
///
/// @since 9.0
/// @author Steve Ebersole
public class BindingCoordinator {
	private final CategorizedDomainModel categorizedDomainModel;
	private final BindingState bindingState;
	private final BindingOptions bindingOptions;
	private final BindingContext bindingContext;

	private final ModelBinders modelBinders;

	/// Create a binding coordinator for a categorized model.
	public BindingCoordinator(
			CategorizedDomainModel categorizedDomainModel,
			BindingState bindingState,
			BindingOptions bindingOptions,
			BindingContext bindingContext) {
		this.categorizedDomainModel = categorizedDomainModel;
		this.bindingOptions = bindingOptions;
		this.bindingState = bindingState;
		this.bindingContext = bindingContext;

		this.modelBinders = new ModelBinders( bindingState, bindingOptions, bindingContext );
	}

	/// Main entry point for binding a categorized domain model.
	///
	/// @param categorizedDomainModel The categorized model to bind
	/// @param state Mutable binding state and produced mapping objects
	/// @param options Binding options in effect
	/// @param bindingContext Access to binding services and shared categorization state
	public static void coordinateBinding(
			CategorizedDomainModel categorizedDomainModel,
			BindingState state,
			BindingOptions options,
			BindingContext bindingContext) {
		final BindingCoordinator coordinator = new BindingCoordinator(
				categorizedDomainModel,
				state,
				options,
				bindingContext
		);

		coordinator.coordinateBinding();
	}

	private void coordinateBinding() {
		// todo : to really work on these, need to changes to MetadataBuildingContext/InFlightMetadataCollector

		coordinateGlobalBindings();
		coordinateModelBindings();
	}

	private void coordinateModelBindings() {
		final List<ManagedTypeBinder> binders = new ArrayList<>();
		final Set<String> boundTypeNames = new HashSet<>();
		registerCategorizedDeclarationContainers();
		categorizedDomainModel.forEachEntityHierarchy( (index, hierarchy) -> {
			hierarchy.forEachType( (type, superType, entityHierarchy, relation) -> {
				if ( type.getManagedTypeKind() != ManagedTypeMetadata.Kind.ENTITY
						|| boundTypeNames.add( type.getClassDetails().getClassName() ) ) {
					binders.add( createIdentifiableTypeBinder( type, superType, entityHierarchy, relation ) );
				}
			} );
			registerEntityHierarchyBinding( hierarchy );
		} );

		runPhase( binders, TypeBindingPhase.Tables.class, TypeBindingPhase.Tables::bindTables );
		runPhase( binders, TypeBindingPhase.SuperType.class, TypeBindingPhase.SuperType::bindSuperType );
		runPhase( binders, TypeBindingPhase.EntityMetadata.class, TypeBindingPhase.EntityMetadata::bindEntityMetadata );
		runPhase( binders, TypeBindingPhase.Identifiers.class, TypeBindingPhase.Identifiers::bindIdentifier );
		runAssociationIdentifierPhase( binders );
		runPhase( binders, TypeBindingPhase.Members.class, TypeBindingPhase.Members::bindMembers );
		runPhase( binders, TypeBindingPhase.CollectionIndexes.class, TypeBindingPhase.CollectionIndexes::bindCollectionIndexes );
		runPhase( binders, TypeBindingPhase.AssociationTargets.class, TypeBindingPhase.AssociationTargets::bindAssociationTargets );
		runPhase( binders, TypeBindingPhase.DerivedIdentifiers.class, TypeBindingPhase.DerivedIdentifiers::bindDerivedIdentifiers );
		runPhase( binders, TypeBindingPhase.TableKeys.class, TypeBindingPhase.TableKeys::bindTableKeys );
		runPhase( binders, TypeBindingPhase.InverseAssociations.class, TypeBindingPhase.InverseAssociations::bindInverseAssociations );
		runPhase( binders, TypeBindingPhase.ForeignKeys.class, TypeBindingPhase.ForeignKeys::bindForeignKeys );

		// process identifiers
		categorizedDomainModel.forEachEntityHierarchy( (index, hierarchy) -> {
			final EntityTypeBinder typeBinder = (EntityTypeBinder) bindingState.getTypeBinder( hierarchy.getRoot() );
			final RootClass binding = (RootClass) typeBinder.getTypeBinding();
			ModelBindingLogging.MODEL_BINDING_LOGGER.tracef( "Bound entity hierarchy - %s", binding.getEntityName() );
		} );
	}

	private void registerCategorizedDeclarationContainers() {
		categorizedDomainModel.forEachMappedSuperclass( (name, type) -> {
			if ( bindingState.getBootBindingModel().getManagedTypeBinding( type ) == null ) {
				bindingState.getBootBindingModel().addManagedTypeBinding(
						new MappedSuperclassTypeBinding( type, defaultAccessType( type ) )
				);
			}
		} );
		categorizedDomainModel.forEachEmbeddable( (name, type) -> {
			if ( bindingState.getBootBindingModel().getManagedTypeBinding( type ) == null ) {
				bindingState.getBootBindingModel().addManagedTypeBinding(
						new EmbeddableTypeBinding( type, defaultAccessType( type ) )
				);
			}
		} );
	}

	private void registerEntityHierarchyBinding(EntityHierarchy hierarchy) {
		final EntityTypeBinding rootBinding = (EntityTypeBinding) bindingState.getBootBindingModel()
				.getManagedTypeBinding( hierarchy.getRoot().getClassDetails() );
		final ManagedTypeBinding absoluteRootBinding = bindingState.getBootBindingModel()
				.getManagedTypeBinding( hierarchy.getAbsoluteRoot().getClassDetails() );
		final List<EntityHierarchyBinding.Type> types = new ArrayList<>();
		hierarchy.forEachType( (type, superType, entityHierarchy, relation) -> {
			final ManagedTypeBinding typeBinding = bindingState.getBootBindingModel()
					.getManagedTypeBinding( type.getClassDetails() );
			final ManagedTypeBinding superTypeBinding = superType == null
					? null
					: bindingState.getBootBindingModel().getManagedTypeBinding( superType.getClassDetails() );
			types.add( new EntityHierarchyBinding.Type(
					typeBinding,
					superTypeBinding,
					toBindingRelation( relation )
			) );
		} );
		bindingState.getBootBindingModel().addEntityHierarchyBinding(
				hierarchy.getRoot(),
				new EntityHierarchyBinding(
						rootBinding,
						absoluteRootBinding,
						types,
						hierarchy.getInheritanceType(),
						hierarchy.getDefaultAccessType(),
						hierarchy.getOptimisticLockStyle()
				)
		);
	}

	private EntityHierarchyBinding.Relation toBindingRelation(EntityHierarchy.HierarchyRelation relation) {
		return switch ( relation ) {
			case SUPER -> EntityHierarchyBinding.Relation.SUPER;
			case ROOT -> EntityHierarchyBinding.Relation.ROOT;
			case SUB -> EntityHierarchyBinding.Relation.SUB;
		};
	}

	private jakarta.persistence.AccessType defaultAccessType(ClassDetails type) {
		final var access = type.getDirectAnnotationUsage( jakarta.persistence.Access.class );
		return access == null ? jakarta.persistence.AccessType.FIELD : access.value();
	}

	private void coordinateGlobalBindings() {
		final GlobalRegistrations globalRegistrations = categorizedDomainModel.getGlobalRegistrations();
		processGenerators( globalRegistrations );
		processImports( globalRegistrations );
		processConverters( globalRegistrations );
		processNamedQueries( globalRegistrations );
		processSqlResultSetMappings( globalRegistrations );
		processNamedEntityGraphs( globalRegistrations );
		processFetchProfiles( globalRegistrations );
		processDatabaseObjects( globalRegistrations );
		processJavaTypeRegistrations( globalRegistrations );
		processJdbcTypeRegistrations( globalRegistrations );
		processCustomTypes( globalRegistrations );
		processInstantiators( globalRegistrations );
		processEventListeners( globalRegistrations );
		processFilterDefinitions( globalRegistrations );
	}

	private <P> void runPhase(List<ManagedTypeBinder> binders, Class<P> phaseType, Consumer<P> phaseAction) {
		binders.forEach( (binder) -> {
			if ( phaseType.isInstance( binder ) ) {
				phaseAction.accept( phaseType.cast( binder ) );
			}
		} );
	}

	private void runAssociationIdentifierPhase(List<ManagedTypeBinder> binders) {
		boolean processedAny;
		do {
			processedAny = false;
			for ( ManagedTypeBinder binder : binders ) {
				if ( binder instanceof TypeBindingPhase.AssociationIdentifiers associationIdentifiers ) {
					processedAny |= associationIdentifiers.bindAssociationIdentifiers();
				}
			}
		}
		while ( processedAny );
	}


	private ManagedTypeBinder createIdentifiableTypeBinder(
			IdentifiableTypeMetadata type,
			IdentifiableTypeMetadata superType,
			EntityHierarchy hierarchy,
			EntityHierarchy.HierarchyRelation relation) {
		processGenerators( type );

		if ( type.getManagedTypeKind() == ManagedTypeMetadata.Kind.ENTITY ) {
			if ( bindingState.getBootBindingModel().getManagedTypeBinding( type.getClassDetails() ) == null ) {
				bindingState.getBootBindingModel().addManagedTypeBinding(
						new EntityTypeBinding( type.getClassDetails(), type.getAccessType() )
				);
			}
			final EntityTypeBinder binder = new EntityTypeBinder(
					(EntityTypeMetadata) type,
					superType,
					relation,
					modelBinders,
					bindingState,
					bindingOptions,
					bindingContext
			);
			bindTypeSkeleton( binder );
			return binder;
		}
		else {
			assert type.getManagedTypeKind() == ManagedTypeMetadata.Kind.MAPPED_SUPER;
			if ( bindingState.getBootBindingModel().getManagedTypeBinding( type.getClassDetails() ) == null ) {
				bindingState.getBootBindingModel().addManagedTypeBinding(
						new MappedSuperclassTypeBinding( type.getClassDetails(), type.getAccessType() )
				);
			}
			final MappedSuperTypeBinder binder = new MappedSuperTypeBinder(
					(MappedSuperclassTypeMetadata) type,
					superType,
					relation,
					modelBinders,
					bindingState,
					bindingOptions,
					bindingContext
			);
			bindTypeSkeleton( binder );
			return binder;
		}
	}

	private void bindTypeSkeleton(ManagedTypeBinder binder) {
		( (TypeBindingPhase.TypeSkeleton) binder ).bindTypeSkeleton();
	}

	private void processGenerators(GlobalRegistrations globalRegistrations) {
		globalRegistrations.getSequenceGeneratorRegistrations().values().forEach( (registration) -> {
			bindingState.addIdentifierGenerator( buildSequenceGeneratorDefinition( registration ) );
		} );
		globalRegistrations.getTableGeneratorRegistrations().values().forEach( (registration) -> {
			bindingState.addIdentifierGenerator( buildTableGeneratorDefinition( registration ) );
		} );
		globalRegistrations.getGenericGeneratorRegistrations().values().forEach( (registration) -> {
			bindingState.addIdentifierGenerator( buildGenericGeneratorDefinition( registration ) );
		} );
	}

	private void processImports(GlobalRegistrations globalRegistrations) {
		globalRegistrations.getImportedRenames().forEach( bindingState::addImport );
	}

	private void processConverters(GlobalRegistrations globalRegistrations) {
		globalRegistrations.getConverterRegistrations().forEach( this::processConverter );
	}

	private void processNamedQueries(GlobalRegistrations globalRegistrations) {
		globalRegistrations.getNamedQueryRegistrations().values().forEach( (registration) -> {
			if ( registration.isJpa() ) {
				if ( registration.getConfiguration() instanceof NamedStatement statement ) {
					QueryBinder.bindStatement(
							statement,
							bindingState.getMetadataBuildingContext(),
							null
					);
				}
				else {
					QueryBinder.bindQuery(
							(jakarta.persistence.NamedQuery) registration.getConfiguration(),
							bindingState.getMetadataBuildingContext(),
							false,
							null
					);
				}
			}
			else {
				QueryBinder.bindQuery(
						(org.hibernate.annotations.NamedQuery) registration.getConfiguration(),
						bindingState.getMetadataBuildingContext(),
						null
				);
			}
		} );
		globalRegistrations.getNamedNativeQueryRegistrations().values().forEach( (registration) -> {
			if ( registration.isJpa() ) {
				if ( registration.getConfiguration() instanceof NamedNativeStatement statement ) {
					QueryBinder.bindNativeStatement(
							statement,
							bindingState.getMetadataBuildingContext(),
							null
					);
				}
				else {
					QueryBinder.bindNativeQuery(
							(jakarta.persistence.NamedNativeQuery) registration.getConfiguration(),
							bindingState.getMetadataBuildingContext(),
							null,
							false
					);
				}
			}
			else {
				QueryBinder.bindNativeQuery(
						(org.hibernate.annotations.NamedNativeQuery) registration.getConfiguration(),
						bindingState.getMetadataBuildingContext(),
						null
				);
			}
		} );
		globalRegistrations.getNamedStoredProcedureQueryRegistrations().values().forEach( (registration) ->
				QueryBinder.bindNamedStoredProcedureQuery(
						(jakarta.persistence.NamedStoredProcedureQuery) registration.getConfiguration(),
						bindingState.getMetadataBuildingContext(),
						false
				)
		);
	}

	private void processSqlResultSetMappings(GlobalRegistrations globalRegistrations) {
		globalRegistrations.getSqlResultSetMappingRegistrations().values().forEach( (registration) ->
				QueryBinder.bindSqlResultSetMapping(
						registration.configuration(),
						bindingState.getMetadataBuildingContext(),
						false
				)
		);
	}

	private void processNamedEntityGraphs(GlobalRegistrations globalRegistrations) {
		globalRegistrations.getNamedEntityGraphRegistrations().values().forEach(
				bindingState::addNamedEntityGraph
		);
	}

	private void processFetchProfiles(GlobalRegistrations globalRegistrations) {
		for ( FetchProfileRegistration registration : globalRegistrations.getFetchProfileRegistrations() ) {
			FetchProfile profile = bindingState.getFetchProfile( registration.getName() );
			if ( profile == null ) {
				profile = new FetchProfile( registration.getName(), MetadataSource.OTHER );
				bindingState.addFetchProfile( profile );
			}
			for ( FetchProfileRegistration.FetchOverride fetchOverride : registration.getFetchOverrides() ) {
				profile.addFetch( new FetchProfile.Fetch(
						fetchOverride.entityName(),
						fetchOverride.association(),
						fetchMode( fetchOverride.style() ),
						FetchType.EAGER
				) );
			}
		}
	}

	private static FetchMode fetchMode(String style) {
		if ( style == null ) {
			return FetchMode.JOIN;
		}
		for ( FetchMode mode : FetchMode.values() ) {
			if ( mode.name().equalsIgnoreCase( style ) ) {
				return mode;
			}
		}
		return FetchMode.JOIN;
	}

	private void processDatabaseObjects(GlobalRegistrations globalRegistrations) {
		globalRegistrations.getDatabaseObjectRegistrations().forEach( (registration) ->
				bindingState.addAuxiliaryDatabaseObject( buildAuxiliaryDatabaseObject( registration ) )
		);
	}

	private AuxiliaryDatabaseObject buildAuxiliaryDatabaseObject(DatabaseObjectRegistration registration) {
		final AuxiliaryDatabaseObject auxiliaryDatabaseObject;
		if ( isNotEmpty( registration.definition() ) ) {
			try {
				auxiliaryDatabaseObject = (AuxiliaryDatabaseObject)
						bindingState.getMetadataBuildingContext()
								.getBootstrapContext()
								.getClassLoaderService()
								.classForName( registration.definition() )
								.getConstructor()
								.newInstance();
			}
			catch (Exception e) {
				throw new MappingException(
						"Unable to instantiate custom AuxiliaryDatabaseObject class [%s]"
								.formatted( registration.definition() ),
						e
				);
			}
		}
		else {
			auxiliaryDatabaseObject = new SimpleAuxiliaryDatabaseObject(
					bindingState.getDatabase().getDefaultNamespace(),
					registration.create(),
					registration.drop(),
					null
			);
		}

		if ( !registration.dialectScopes().isEmpty()
				&& auxiliaryDatabaseObject instanceof AuxiliaryDatabaseObject.Expandable expandable ) {
			registration.dialectScopes().forEach( (dialectScope) -> expandable.addDialectScope( dialectScope.name() ) );
		}

		return auxiliaryDatabaseObject;
	}

	private void processJavaTypeRegistrations(GlobalRegistrations globalRegistrations) {
		globalRegistrations.getJavaTypeRegistrations().forEach( this::processJavaTypeRegistration );
	}

	private void processJdbcTypeRegistrations(GlobalRegistrations globalRegistrations) {
		globalRegistrations.getJdbcTypeRegistrations().forEach( this::processJdbcTypeRegistration );
	}

	private void processCustomTypes(GlobalRegistrations globalRegistrations) {
		globalRegistrations.getUserTypeRegistrations().forEach( this::processUserTypeRegistration );
		globalRegistrations.getCompositeUserTypeRegistrations().forEach( this::processCompositeUserTypeRegistration );
		globalRegistrations.getCollectionTypeRegistrations().forEach( this::processCollectionTypeRegistration );
	}

	private void processInstantiators(GlobalRegistrations globalRegistrations) {
		globalRegistrations.getEmbeddableInstantiatorRegistrations().forEach( this::processEmbeddableInstantiatorRegistration );
	}

	private void processEventListeners(GlobalRegistrations globalRegistrations) {
		// JPA event listeners are consumed by EntityTypeMetadata#getCompleteJpaEventListeners()
		// during entity metadata binding.  There is no separate mapping collector
		// registration to apply here.
	}

	private void processFilterDefinitions(GlobalRegistrations globalRegistrations) {
		globalRegistrations.getFilterDefRegistrations().forEach( (s, filterDefRegistration) -> {
			bindingState.apply( filterDefRegistration );
		} );

	}

	private IdentifierGeneratorDefinition buildSequenceGeneratorDefinition(SequenceGeneratorRegistration registration) {
		final IdentifierGeneratorDefinition.Builder definitionBuilder = new IdentifierGeneratorDefinition.Builder();
		GeneratorParameters.interpretSequenceGenerator( registration.configuration(), definitionBuilder );
		return definitionBuilder.build();
	}

	private IdentifierGeneratorDefinition buildTableGeneratorDefinition(TableGeneratorRegistration registration) {
		final IdentifierGeneratorDefinition.Builder definitionBuilder = new IdentifierGeneratorDefinition.Builder();
		GeneratorParameters.interpretTableGenerator( registration.configuration(), definitionBuilder );
		return definitionBuilder.build();
	}

	@SuppressWarnings("removal")
	private IdentifierGeneratorDefinition buildGenericGeneratorDefinition(GenericGeneratorRegistration registration) {
		final IdentifierGeneratorDefinition.Builder definitionBuilder = new IdentifierGeneratorDefinition.Builder();
		definitionBuilder.setName( registration.name() );
		if ( isNotEmpty( registration.strategy() ) ) {
			definitionBuilder.setStrategy( registration.strategy() );
		}
		definitionBuilder.addParams( registration.parameters() );
		return definitionBuilder.build();
	}

	private void processConverter(ConversionRegistration registration) {
		if ( registration.explicitDomainType() == null ) {
			bindingState.addAttributeConverter( attributeConverterClass( registration.converterType() ) );
			return;
		}
		bindingState.addRegisteredConversion( new RegisteredConversion(
				registration.explicitDomainType().toJavaClass(),
				attributeConverterClass( registration.converterType() ),
				registration.autoApply()
		) );
	}

	private void processJavaTypeRegistration(JavaTypeRegistration registration) {
		bindingState.addJavaTypeRegistration(
				registration.domainType().toJavaClass(),
				instantiate( registration.descriptor(), JavaType.class, "Java type descriptor" )
		);
	}

	private void processJdbcTypeRegistration(JdbcTypeRegistration registration) {
		bindingState.addJdbcTypeRegistration(
				registration.code(),
				instantiate( registration.descriptor(), JdbcType.class, "JDBC type descriptor" )
		);
	}

	private void processUserTypeRegistration(UserTypeRegistration registration) {
		bindingState.registerUserType(
				registration.domainClass().toJavaClass(),
				userTypeClass( registration.userTypeClass() )
		);
	}

	private void processCompositeUserTypeRegistration(CompositeUserTypeRegistration registration) {
		bindingState.registerCompositeUserType(
				registration.embeddableClass().toJavaClass(),
				compositeUserTypeClass( registration.userTypeClass() )
		);
	}

	private void processCollectionTypeRegistration(CollectionTypeRegistration registration) {
		bindingState.addCollectionTypeRegistration(
				registration.classification(),
				instantiateClass( registration.userTypeClass(), UserCollectionType.class, "collection user type" ),
				registration.parameterMap()
		);
	}

	private void processEmbeddableInstantiatorRegistration(EmbeddableInstantiatorRegistration registration) {
		bindingState.registerEmbeddableInstantiator(
				registration.embeddableClass().toJavaClass(),
				instantiateClass( registration.instantiator(), EmbeddableInstantiator.class, "embeddable instantiator" )
		);
	}

	private <T> T instantiate(ClassDetails classDetails, Class<T> expectedType, String registrationRole) {
		final Class<? extends T> javaClass = instantiateClass( classDetails, expectedType, registrationRole );
		try {
			return javaClass.getConstructor().newInstance();
		}
		catch (Exception e) {
			final ModelsException modelsException = new ModelsException(
					"Error instantiating global " + registrationRole + " registration - " + classDetails.getName()
			);
			modelsException.addSuppressed( e );
			throw modelsException;
		}
	}

	@SuppressWarnings("unchecked")
	private <T> Class<? extends T> instantiateClass(ClassDetails classDetails, Class<T> expectedType, String registrationRole) {
		final Class<?> javaClass = classDetails.toJavaClass();
		if ( !expectedType.isAssignableFrom( javaClass ) ) {
			throw new ModelsException(
					"Global " + registrationRole + " registration class `" + classDetails.getName()
							+ "` did not implement " + expectedType.getName()
			);
		}
		return (Class<? extends T>) javaClass;
	}

	@SuppressWarnings("unchecked")
	private Class<? extends UserType<?>> userTypeClass(ClassDetails classDetails) {
		return (Class<? extends UserType<?>>) (Class<?>) instantiateClass( classDetails, UserType.class, "user type" );
	}

	@SuppressWarnings("unchecked")
	private Class<? extends CompositeUserType<?>> compositeUserTypeClass(ClassDetails classDetails) {
		return (Class<? extends CompositeUserType<?>>) (Class<?>) instantiateClass(
				classDetails,
				CompositeUserType.class,
				"composite user type"
		);
	}

	@SuppressWarnings("unchecked")
	private Class<? extends AttributeConverter<?, ?>> attributeConverterClass(ClassDetails classDetails) {
		return (Class<? extends AttributeConverter<?, ?>>) (Class<?>) instantiateClass(
				classDetails,
				AttributeConverter.class,
				"attribute converter"
		);
	}

	private void processTables(AttributeMetadata attribute) {
		final JoinTable joinTableAnn = attribute.getMember().getDirectAnnotationUsage( JoinTable.class );
		final CollectionTable collectionTableAnn = attribute.getMember().getDirectAnnotationUsage( CollectionTable.class );

		final OneToOne oneToOneAnn = attribute.getMember().getDirectAnnotationUsage( OneToOne.class );
		final ManyToOne manyToOneAnn = attribute.getMember().getDirectAnnotationUsage( ManyToOne.class );
		final ElementCollection elementCollectionAnn = attribute.getMember().getDirectAnnotationUsage( ElementCollection.class );
		final OneToMany oneToManyAnn = attribute.getMember().getDirectAnnotationUsage( OneToMany.class );
		final Any anyAnn = attribute.getMember().getDirectAnnotationUsage( Any.class );
		final ManyToAny manyToAnyAnn = attribute.getMember().getDirectAnnotationUsage( ManyToAny.class );

		final boolean hasAnyTableAnnotations = joinTableAnn != null
				|| collectionTableAnn != null;

		final boolean hasAnyAssociationAnnotations = oneToOneAnn != null
				|| manyToOneAnn != null
				|| elementCollectionAnn != null
				|| oneToManyAnn != null
				|| anyAnn != null
				|| manyToAnyAnn != null;

		if ( !hasAnyAssociationAnnotations ) {
			if ( hasAnyTableAnnotations ) {
				throw new AnnotationPlacementException(
						"@JoinTable or @CollectionTable used on non-association attribute - " + attribute.getMember()
				);
			}
		}

		if ( elementCollectionAnn != null ) {
			if ( joinTableAnn != null ) {
				throw new AnnotationPlacementException(
						"@JoinTable should not be used with @ElementCollection; use @CollectionTable instead - " + attribute.getMember()
				);
			}

			// an element-collection "owns" the collection table, so create it right away

		}

		// ^^ accounting for owning v. "inverse" side
		//
		// on the owning side we get/create the reference and configure it
		//
		// on the inverse side we just get the reference.
		//
		// a cool idea here for "smarter second-pass"... on the inverse side -
		// 		TableReference mappedTable = bindingState.
		//

	}

	private void processGenerators(IdentifiableTypeMetadata type) {
		final ClassDetails typeClassDetails = type.getClassDetails();

		final TableGenerator[] tableGenerators = typeClassDetails.getRepeatedAnnotationUsages(
				TableGenerator.class,
				bindingContext.getBootstrapContext().getModelsContext()
		);
		for ( TableGenerator tableGeneratorAnn : tableGenerators ) {
			// process both the table and the generator
		}

		final SequenceGenerator[] sequenceGenerators = typeClassDetails.getRepeatedAnnotationUsages(
				SequenceGenerator.class,
				bindingContext.getBootstrapContext().getModelsContext()
		);
		for ( SequenceGenerator sequenceGeneratorAnn : sequenceGenerators ) {
			// process both the sequence and the generator
		}

	}

}
