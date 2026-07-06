/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.context;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import jakarta.annotation.Nonnull;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.internal.FilterDefBinder;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.mapping.internal.model.BootBindingModel;
import org.hibernate.boot.mapping.internal.binders.AssociationTableBinding;
import org.hibernate.boot.mapping.internal.binders.AssociationIdentifierBinding;
import org.hibernate.boot.mapping.internal.binders.AssociationTargetBinding;
import org.hibernate.boot.mapping.internal.binders.AttributeBindingPhase;
import org.hibernate.boot.mapping.internal.binders.CollectionTableBinding;
import org.hibernate.boot.mapping.internal.binders.ComponentBindingPhase;
import org.hibernate.boot.mapping.internal.binders.DerivedIdentifierBinding;
import org.hibernate.boot.mapping.internal.binders.EntityTypeBinder;
import org.hibernate.boot.mapping.internal.binders.ForeignKeyBinding;
import org.hibernate.boot.mapping.internal.binders.IdentifierBinding;
import org.hibernate.boot.mapping.internal.binders.IdentifiableTypeBinder;
import org.hibernate.boot.mapping.internal.binders.InversePluralAssociationBinding;
import org.hibernate.boot.mapping.internal.binders.InverseToOneAssociationBinding;
import org.hibernate.boot.mapping.internal.binders.ManagedTypeBinder;
import org.hibernate.boot.mapping.internal.binders.MappedSuperTypeBinder;
import org.hibernate.boot.mapping.internal.binders.PropertyMapKeyBinding;
import org.hibernate.boot.mapping.internal.binders.StateManagementBindingPhase;
import org.hibernate.boot.mapping.internal.binders.TableForeignKeyBinding;
import org.hibernate.boot.mapping.internal.relational.RelationalModelCorrespondences;
import org.hibernate.boot.mapping.internal.relational.SecondaryTable;
import org.hibernate.boot.mapping.internal.relational.TableOwner;
import org.hibernate.boot.mapping.internal.relational.TableReference;
import org.hibernate.boot.mapping.internal.view.CollationContributionView;
import org.hibernate.boot.mapping.internal.view.EmbeddableContributionView;
import org.hibernate.boot.mapping.internal.view.MappedSuperclassContributionView;
import org.hibernate.boot.mapping.internal.view.NaturalIdContributionView;
import org.hibernate.boot.mapping.internal.view.TenantIdBindingView;
import org.hibernate.boot.mapping.internal.view.VersionBindingView;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.FilterDefRegistration;
import org.hibernate.boot.mapping.internal.categorize.IdentifiableTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.ManagedTypeMetadata;
import org.hibernate.boot.query.NamedResultSetMappingDescriptor;
import org.hibernate.boot.spi.InFlightMetadataCollector.CollectionTypeRegistrationDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.util.KeyedConsumer;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DenormalizedTable;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserCollectionType;
import org.hibernate.usertype.UserType;

import jakarta.persistence.AttributeConverter;

/// Mutable binding-state implementation shared by all coordinator phases.
///
/// `BindingStateImpl` is the local registry for objects that need to be visible
/// across binders without falling back to global metadata-collector lookups.  It
/// tracks:
///
/// - type binders, keyed by their Hibernate Models [ClassDetails]
/// - logical and physical table references
/// - identifier shapes produced by the identifier phase
/// - typed pending work for later phases, such as association targets, collection
///   table keys, inverse associations, derived identifiers, and foreign keys
///
/// This object is intentionally mutable because binding is incremental.  The
/// important boundary is that the state is typed and phase-specific; each list is
/// consumed by a named phase instead of being an opaque "try again later"
/// callback queue.
///
/// @since 9.0
/// @author Steve Ebersole
public class BindingStateImpl implements BindingState {
	private MetadataBuildingContext metadataBuildingContext;
	private final MetadataCollector metadataCollector;
	private final BootBindingModel bootBindingModel = new BootBindingModel();
	private final RelationalModelCorrespondences relationalModelCorrespondences;

	private final Database database;
	private final JdbcServices jdbcServices;

	private final Map<String, TableReference> tableMap = new HashMap<>();
	private final Map<TableOwner, TableReference> tableByOwnerMap = new HashMap<>();
	private final Map<org.hibernate.mapping.Table, TableReference> tableByBindingMap = new IdentityHashMap<>();
	private final Map<org.hibernate.mapping.Table, SecondaryTable> secondaryTableByBinding = new HashMap<>();
	private final Map<Join, AssociationTableBinding> associationTableBindings = new HashMap<>();
	private final java.util.List<CollectionTableBinding> collectionTableBindings = new java.util.ArrayList<>();
	private final java.util.List<PropertyMapKeyBinding> propertyMapKeyBindings = new java.util.ArrayList<>();
	private final java.util.List<AssociationIdentifierBinding> associationIdentifierBindings = new java.util.ArrayList<>();
	private final java.util.List<AssociationTargetBinding> associationTargetBindings = new java.util.ArrayList<>();
	private final java.util.List<DerivedIdentifierBinding> derivedIdentifierBindings = new java.util.ArrayList<>();
	private final java.util.List<InversePluralAssociationBinding> inversePluralAssociationBindings = new java.util.ArrayList<>();
	private final java.util.List<InverseToOneAssociationBinding> inverseToOneAssociationBindings = new java.util.ArrayList<>();
	private final java.util.List<ForeignKeyBinding> foreignKeyBindings = new java.util.ArrayList<>();
	private final java.util.List<TableForeignKeyBinding> tableForeignKeyBindings = new java.util.ArrayList<>();
	private final java.util.List<ComponentBindingPhase.CustomMapping> componentCustomMappings = new java.util.ArrayList<>();
	private final java.util.List<ComponentBindingPhase.AggregateFinalization> componentAggregateFinalizations =
			new java.util.ArrayList<>();
	private final java.util.List<AttributeBindingPhase.CustomMapping> attributeCustomMappings = new java.util.ArrayList<>();
	private final java.util.List<AttributeBindingPhase.ValueResolution> attributeValueResolutions = new java.util.ArrayList<>();
	private final java.util.List<AttributeBindingPhase.PostValueResolution> postAttributeValueResolutions = new java.util.ArrayList<>();
	private final java.util.List<StateManagementBindingPhase.RootEntity> stateManagementRootBindings =
			new java.util.ArrayList<>();
	private final java.util.List<StateManagementBindingPhase.PropertyExclusions> stateManagementPropertyBindings =
			new java.util.ArrayList<>();
	private final Map<Property, MappedSuperclassPropertyHandoff> mappedSuperclassPropertyHandoffs =
			new IdentityHashMap<>();
	private final Map<Property, NaturalIdPropertyHandoff> naturalIdPropertyHandoffs =
			new IdentityHashMap<>();
	private final java.util.List<NaturalIdPropertyHandoff> naturalIdPropertyHandoffList =
			new java.util.ArrayList<>();
	private final Map<Property, CollationPropertyHandoff> collationPropertyHandoffs =
			new IdentityHashMap<>();
	private final java.util.List<CollationPropertyHandoff> collationPropertyHandoffList =
			new java.util.ArrayList<>();
	private final Map<RootClass, TenantIdPropertyHandoff> tenantIdPropertyHandoffsByRoot =
			new IdentityHashMap<>();
	private final Map<Property, TenantIdPropertyHandoff> tenantIdPropertyHandoffsByProperty =
			new IdentityHashMap<>();
	private final java.util.List<TenantIdPropertyHandoff> tenantIdPropertyHandoffList =
			new java.util.ArrayList<>();
	private final Map<RootClass, VersionPropertyHandoff> versionPropertyHandoffsByRoot =
			new IdentityHashMap<>();
	private final Map<Property, VersionPropertyHandoff> versionPropertyHandoffsByProperty =
			new IdentityHashMap<>();
	private final java.util.List<VersionPropertyHandoff> versionPropertyHandoffList =
			new java.util.ArrayList<>();
	private final Map<Component, EmbeddableComponentHandoff> embeddableComponentHandoffs =
			new IdentityHashMap<>();
	private final java.util.List<EmbeddableComponentHandoff> embeddableComponentHandoffList =
			new java.util.ArrayList<>();
	private final java.util.List<StateManagementBindingPhase.CollectionMapping> stateManagementCollectionBindings =
			new java.util.ArrayList<>();
	private final java.util.List<StateManagementBindingPhase.OneToManyAuditCollection> stateManagementOneToManyCollectionBindings =
			new java.util.ArrayList<>();
	private final java.util.List<StateManagementBindingPhase.Finalizer> stateManagementFinalizers =
			new java.util.ArrayList<>();

	private final Map<ClassDetails, ManagedTypeBinder> typeBinders = new HashMap<>();
	private final Map<ClassDetails, IdentifiableTypeBinder> typeBindersBySuper = new HashMap<>();
	private final Map<EntityTypeMetadata, IdentifierBinding> entityIdentifierBindings = new HashMap<>();
	private final Map<RootClass, EntityIdentifierHandoff> entityIdentifierHandoffsByRoot =
			new IdentityHashMap<>();
	private final Map<KeyValue, EntityIdentifierHandoff> entityIdentifierHandoffsByValue =
			new IdentityHashMap<>();
	private final java.util.List<EntityIdentifierHandoff> entityIdentifierHandoffList =
			new java.util.ArrayList<>();

	public BindingStateImpl(MetadataBuildingContext metadataBuildingContext, MetadataCollector metadataCollector) {
		this.metadataBuildingContext = metadataBuildingContext;
		this.metadataCollector = metadataCollector;
		this.database = metadataCollector.getDatabase();
		this.relationalModelCorrespondences = new RelationalModelCorrespondences( database );
		this.jdbcServices = metadataBuildingContext.getJdbcServices();
	}

	public MetadataBuildingContext useMetadataBuildingContext(MetadataBuildingContext metadataBuildingContext) {
		final MetadataBuildingContext previous = this.metadataBuildingContext;
		this.metadataBuildingContext = metadataBuildingContext;
		return previous;
	}

	@Override
	public MetadataBuildingContext getMetadataBuildingContext() {
		return metadataBuildingContext;
	}

	@Override @Nonnull
	public Database getDatabase() {
		return database;
	}

	@Override @Nonnull
	public RelationalModelCorrespondences getRelationalModelCorrespondences() {
		return relationalModelCorrespondences;
	}

	@Override @Nonnull
	public Table getOrCreateTable(
			String schema,
			String catalog,
			String name,
			String subselect,
			boolean isAbstract,
			boolean isExplicit) {
		return metadataCollector.getOrCreateTable(
				schema,
				catalog,
				name,
				subselect,
				isAbstract,
				metadataBuildingContext,
				isExplicit
		);
	}

	@Override @Nonnull
	public DenormalizedTable createDenormalizedTable(
			String schema,
			String catalog,
			String name,
			boolean isAbstract,
			String subselect,
			Table includedTable) {
		return metadataCollector.createDenormalizedTable(
				schema,
				catalog,
				name,
				isAbstract,
				subselect,
				includedTable,
				metadataBuildingContext
		);
	}

	@Override @Nonnull
	public JdbcServices getJdbcServices() {
		return jdbcServices;
	}

	@Override @Nonnull
	public TypeConfiguration getTypeConfiguration() {
		return metadataBuildingContext.getTypeConfiguration();
	}

	@Override @Nonnull
	public BootBindingModel getBootBindingModel() {
		return bootBindingModel;
	}

	@Override
	public void addEntityBinding(PersistentClass entityBinding) {
		metadataCollector.addEntityBinding( entityBinding );
	}

	@Override
	public PersistentClass getEntityBinding(String entityName) {
		return metadataCollector.getEntityBinding( entityName );
	}

	@Override @Nonnull
	public Iterable<PersistentClass> getEntityBindings() {
		return metadataCollector.getEntityBindings();
	}

	@Override
	public void addMappedSuperclass(Class<?> mappedSuperclassClass, MappedSuperclass mappedSuperclass) {
		metadataCollector.addMappedSuperclass( mappedSuperclassClass, mappedSuperclass );
	}

	@Override
	public void addCollectionBinding(Collection collection) {
		metadataCollector.addCollectionBinding( collection );
	}

	@Override
	public void addImport(String importName, String entityName) {
		metadataCollector.addImport( importName, entityName );
	}

	@Override
	public String getImport(String importName) {
		return metadataCollector.getImport( importName );
	}

	@Override
	public void addUniquePropertyReference(String referencedEntityName, String referencedPropertyName) {
		metadataCollector.addUniquePropertyReference( referencedEntityName, referencedPropertyName );
	}

	@Override
	public void addPropertyReference(String referencedEntityName, String referencedPropertyName) {
		metadataCollector.addPropertyReference( referencedEntityName, referencedPropertyName );
	}

	@Override
	public void addIdentifierGenerator(IdentifierGeneratorDefinition identifierGeneratorDefinition) {
		metadataCollector.addIdentifierGenerator( identifierGeneratorDefinition );
	}

	@Override
	public void addNamedEntityGraph(NamedEntityGraphDefinition namedEntityGraphDefinition) {
		metadataCollector.addNamedEntityGraph( namedEntityGraphDefinition );
	}

	@Override
	public void addResultSetMapping(NamedResultSetMappingDescriptor resultSetMappingDescriptor) {
		metadataCollector.addResultSetMapping( resultSetMappingDescriptor );
	}

	@Override
	public void addDefaultResultSetMapping(NamedResultSetMappingDescriptor resultSetMappingDescriptor) {
		metadataCollector.addDefaultResultSetMapping( resultSetMappingDescriptor );
	}

	@Override
	public void addFetchProfile(FetchProfile fetchProfile) {
		metadataCollector.addFetchProfile( fetchProfile );
	}

	@Override
	public FetchProfile getFetchProfile(String name) {
		return metadataCollector.getFetchProfile( name );
	}

	@Override
	public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		metadataCollector.addAuxiliaryDatabaseObject( auxiliaryDatabaseObject );
	}

	@Override
	public void addAttributeConverter(Class<? extends AttributeConverter<?, ?>> converterClass) {
		metadataCollector.addAttributeConverter( converterClass );
	}

	@Override
	public void addAttributeConverter(ConverterDescriptor<?, ?> converterDescriptor) {
		metadataCollector.addAttributeConverter( converterDescriptor );
	}

	@Override
	public void addRegisteredConversion(RegisteredConversion registeredConversion) {
		metadataCollector.addRegisteredConversion( registeredConversion );
	}

	@Override
	public void addJavaTypeRegistration(Class<?> domainType, JavaType<?> descriptor) {
		metadataCollector.addJavaTypeRegistration( domainType, descriptor );
	}

	@Override
	public void addJdbcTypeRegistration(int code, JdbcType descriptor) {
		metadataCollector.addJdbcTypeRegistration( code, descriptor );
	}

	@Override
	public void registerUserType(Class<?> domainClass, Class<? extends UserType<?>> userTypeClass) {
		metadataCollector.registerUserType( domainClass, userTypeClass );
	}

	@Override
	public Class<? extends UserType<?>> findRegisteredUserType(Class<?> domainClass) {
		return metadataCollector.findRegisteredUserType( domainClass );
	}

	@Override
	public void registerCompositeUserType(Class<?> embeddableClass, Class<? extends CompositeUserType<?>> userTypeClass) {
		metadataCollector.registerCompositeUserType( embeddableClass, userTypeClass );
	}

	@Override
	public Class<? extends CompositeUserType<?>> findRegisteredCompositeUserType(Class<?> embeddableClass) {
		return metadataCollector.findRegisteredCompositeUserType( embeddableClass );
	}

	@Override
	public void addCollectionTypeRegistration(
			CollectionClassification classification,
			Class<? extends UserCollectionType> userTypeClass,
			Map<String, String> parameters) {
		metadataCollector.addCollectionTypeRegistration( classification, userTypeClass, parameters );
	}

	@Override
	public CollectionTypeRegistrationDescriptor findCollectionTypeRegistration(CollectionClassification classification) {
		return metadataCollector.findCollectionTypeRegistration( classification );
	}

	@Override
	public void registerEmbeddableInstantiator(
			Class<?> embeddableClass,
			Class<? extends EmbeddableInstantiator> instantiatorClass) {
		metadataCollector.registerEmbeddableInstantiator( embeddableClass, instantiatorClass );
	}

	@Override
	public FilterDefinition getFilterDefinition(String name) {
		return metadataCollector.getFilterDefinition( name );
	}

	@Override
	public void addFilterDefinition(FilterDefinition filterDefinition) {
		metadataCollector.addFilterDefinition( filterDefinition );
	}

	@Override
	public void registerTypeBinder(ManagedTypeMetadata type, ManagedTypeBinder binder) {
		typeBinders.put( type.getClassDetails(), binder );

		if ( type instanceof IdentifiableTypeMetadata identifiableType ) {
			if ( identifiableType.getSuperType() != null ) {
				typeBindersBySuper.put(
						identifiableType.getSuperType().getClassDetails(),
						(IdentifiableTypeBinder) binder
				);
			}
		}

		if ( binder instanceof EntityTypeBinder entityTypeBinder ) {
			addEntityBinding( entityTypeBinder.getTypeBinding() );
		}
		else if ( binder instanceof MappedSuperTypeBinder mappedSuperBinder ) {
			addMappedSuperclass(
					mappedSuperBinder.getManagedType().getClassDetails().toJavaClass(),
					mappedSuperBinder.getTypeBinding()
			);
		}
	}

	@Override
	public ManagedTypeBinder getTypeBinder(ClassDetails type) {
		return typeBinders.get( type );
	}

	@Override
	public IdentifiableTypeBinder getSuperTypeBinder(ClassDetails type) {
		return typeBindersBySuper.get( type );
	}

	@Override
	public void addIdentifierBinding(EntityTypeMetadata rootType, IdentifierBinding entityIdentifierBinding) {
		entityIdentifierBindings.put( rootType, entityIdentifierBinding );
	}

	@Override
	public IdentifierBinding getIdentifierBinding(EntityTypeMetadata rootType) {
		return entityIdentifierBindings.get( rootType );
	}

	@Override
	public void addEntityIdentifierHandoff(EntityIdentifierHandoff handoff) {
		entityIdentifierHandoffsByRoot.put( handoff.rootClass(), handoff );
		entityIdentifierHandoffsByValue.put( handoff.value(), handoff );
		entityIdentifierHandoffList.add( handoff );
	}

	@Override
	public EntityIdentifierHandoff getEntityIdentifierHandoff(RootClass rootClass) {
		return entityIdentifierHandoffsByRoot.get( rootClass );
	}

	@Override
	public EntityIdentifierHandoff getEntityIdentifierHandoff(KeyValue identifierValue) {
		return entityIdentifierHandoffsByValue.get( identifierValue );
	}

	@Override
	public List<EntityIdentifierHandoff> getEntityIdentifierHandoffs() {
		return List.copyOf( entityIdentifierHandoffList );
	}

	@Override
	public void addComponentCustomMapping(ComponentBindingPhase.CustomMapping binding) {
		componentCustomMappings.add( binding );
	}

	@Override
	public void runComponentCustomMappings() {
		final var pendingBindings = List.copyOf( componentCustomMappings );
		componentCustomMappings.clear();
		pendingBindings.forEach( ComponentBindingPhase.CustomMapping::bindCustomMapping );
	}

	@Override
	public void addComponentAggregateFinalization(ComponentBindingPhase.AggregateFinalization binding) {
		componentAggregateFinalizations.add( binding );
	}

	@Override
	public void runComponentAggregateFinalizations() {
		final var pendingBindings = List.copyOf( componentAggregateFinalizations );
		componentAggregateFinalizations.clear();
		pendingBindings.forEach( ComponentBindingPhase.AggregateFinalization::finishAggregateMapping );
	}

	@Override
	public void addAttributeCustomMapping(AttributeBindingPhase.CustomMapping binding) {
		attributeCustomMappings.add( binding );
	}

	@Override
	public void runAttributeCustomMappings() {
		final var pendingBindings = List.copyOf( attributeCustomMappings );
		attributeCustomMappings.clear();
		pendingBindings.forEach( AttributeBindingPhase.CustomMapping::bindCustomMapping );
	}

	@Override
	public void addAttributeValueResolution(AttributeBindingPhase.ValueResolution binding) {
		attributeValueResolutions.add( binding );
	}

	@Override
	public void addPostAttributeValueResolution(AttributeBindingPhase.PostValueResolution binding) {
		postAttributeValueResolutions.add( binding );
	}

	@Override
	public void runAttributeValueResolutions() {
		final var pendingValueResolutionBindings = List.copyOf( attributeValueResolutions );
		attributeValueResolutions.clear();
		pendingValueResolutionBindings.forEach( AttributeBindingPhase.ValueResolution::resolveValue );

		final var pendingPostValueResolutionBindings = List.copyOf( postAttributeValueResolutions );
		postAttributeValueResolutions.clear();
		pendingPostValueResolutionBindings.forEach( AttributeBindingPhase.PostValueResolution::afterValueResolution );
	}

	@Override
	public void forEachType(KeyedConsumer<String,ManagedTypeBinder> consumer) {
		typeBinders.forEach( (classDetails, managedTypeBinder) -> consumer.accept( classDetails.getName(), managedTypeBinder ) );
	}

	@Override
	public int getTableCount() {
		return tableMap.size();
	}

	@Override
	public void forEachTable(KeyedConsumer<String,TableReference> consumer) {
		//noinspection unchecked
		tableMap.forEach( (BiConsumer<? super String, ? super TableReference>) consumer );
	}

	@Override
	public <T extends TableReference> T getTableByName(String name) {
		//noinspection unchecked
		return (T) tableMap.get( name );
	}

	@Override
	public <T extends TableReference> T getTableByOwner(TableOwner owner) {
		//noinspection unchecked
		return (T) tableByOwnerMap.get( owner );
	}

	@Override
	public <T extends TableReference> T getTableByBinding(org.hibernate.mapping.Table table) {
		//noinspection unchecked
		return (T) tableByBindingMap.get( table );
	}

	@Override
	public void addTable(TableOwner owner, TableReference table) {
		tableMap.put( table.logicalName().getCanonicalName(), table );
		addTableBinding( table );
		tableByOwnerMap.put( owner, table );
	}

	@Override
	public void addTableBinding(TableReference table) {
		tableByBindingMap.put( table.binding(), table );
	}

	@Override
	public void addSecondaryTable(SecondaryTable table) {
		tableMap.put( table.logicalName().getCanonicalName(), table );
		addTableBinding( table );
		secondaryTableByBinding.put( table.binding(), table );
	}

	@Override
	public SecondaryTable getSecondaryTable(org.hibernate.mapping.Table table) {
		return secondaryTableByBinding.get( table );
	}

	@Override
	public void addAssociationTableBinding(AssociationTableBinding associationTableBinding) {
		associationTableBindings.put( associationTableBinding.join(), associationTableBinding );
	}

	@Override
	public AssociationTableBinding getAssociationTableBinding(Join join) {
		return associationTableBindings.get( join );
	}

	@Override
	public void addCollectionTableBinding(CollectionTableBinding collectionTableBinding) {
		collectionTableBindings.add( collectionTableBinding );
	}

	@Override
	public void forEachCollectionTableBinding(java.util.function.Consumer<CollectionTableBinding> consumer) {
		collectionTableBindings.forEach( consumer );
	}

	@Override
	public void addPropertyMapKeyBinding(PropertyMapKeyBinding propertyMapKeyBinding) {
		propertyMapKeyBindings.add( propertyMapKeyBinding );
	}

	@Override
	public void forEachPropertyMapKeyBinding(java.util.function.Consumer<PropertyMapKeyBinding> consumer) {
		propertyMapKeyBindings.forEach( consumer );
	}

	@Override
	public void addAssociationIdentifierBinding(AssociationIdentifierBinding associationIdentifierBinding) {
		associationIdentifierBindings.add( associationIdentifierBinding );
	}

	@Override
	public void forEachAssociationIdentifierBinding(java.util.function.Consumer<AssociationIdentifierBinding> consumer) {
		associationIdentifierBindings.forEach( consumer );
	}

	@Override
	public void addAssociationTargetBinding(AssociationTargetBinding associationTargetBinding) {
		associationTargetBindings.add( associationTargetBinding );
	}

	@Override
	public void forEachAssociationTargetBinding(java.util.function.Consumer<AssociationTargetBinding> consumer) {
		associationTargetBindings.forEach( consumer );
	}

	@Override
	public void addDerivedIdentifierBinding(DerivedIdentifierBinding derivedIdentifierBinding) {
		derivedIdentifierBindings.add( derivedIdentifierBinding );
	}

	@Override
	public void forEachDerivedIdentifierBinding(java.util.function.Consumer<DerivedIdentifierBinding> consumer) {
		derivedIdentifierBindings.forEach( consumer );
	}

	@Override
	public void addInversePluralAssociationBinding(InversePluralAssociationBinding inversePluralAssociationBinding) {
		inversePluralAssociationBindings.add( inversePluralAssociationBinding );
	}

	@Override
	public void forEachInversePluralAssociationBinding(java.util.function.Consumer<InversePluralAssociationBinding> consumer) {
		inversePluralAssociationBindings.forEach( consumer );
	}

	@Override
	public void addInverseToOneAssociationBinding(InverseToOneAssociationBinding inverseToOneAssociationBinding) {
		inverseToOneAssociationBindings.add( inverseToOneAssociationBinding );
	}

	@Override
	public void forEachInverseToOneAssociationBinding(java.util.function.Consumer<InverseToOneAssociationBinding> consumer) {
		inverseToOneAssociationBindings.forEach( consumer );
	}

	@Override
	public void addForeignKeyBinding(ForeignKeyBinding foreignKeyBinding) {
		foreignKeyBindings.add( foreignKeyBinding );
	}

	@Override
	public void forEachForeignKeyBinding(java.util.function.Consumer<ForeignKeyBinding> consumer) {
		foreignKeyBindings.forEach( consumer );
	}

	@Override
	public void addTableForeignKeyBinding(TableForeignKeyBinding tableForeignKeyBinding) {
		tableForeignKeyBindings.add( tableForeignKeyBinding );
	}

	@Override
	public void forEachTableForeignKeyBinding(java.util.function.Consumer<TableForeignKeyBinding> consumer) {
		tableForeignKeyBindings.forEach( consumer );
	}

	@Override
	public void addStateManagementRootBinding(StateManagementBindingPhase.RootEntity binding) {
		stateManagementRootBindings.add( binding );
	}

	@Override
	public void runStateManagementRootBindings() {
		final var bindings = List.copyOf( stateManagementRootBindings );
		stateManagementRootBindings.clear();
		bindings.forEach( StateManagementBindingPhase.RootEntity::bindRootEntity );
	}

	@Override
	public void addStateManagementPropertyBinding(StateManagementBindingPhase.PropertyExclusions binding) {
		stateManagementPropertyBindings.add( binding );
	}

	@Override
	public void runStateManagementPropertyAndCollectionBindings() {
		final var propertyBindings = List.copyOf( stateManagementPropertyBindings );
		final var collectionBindings = List.copyOf( stateManagementCollectionBindings );
		final var oneToManyCollectionBindings = List.copyOf( stateManagementOneToManyCollectionBindings );
		stateManagementPropertyBindings.clear();
		stateManagementCollectionBindings.clear();
		stateManagementOneToManyCollectionBindings.clear();
		propertyBindings.forEach( StateManagementBindingPhase.PropertyExclusions::bindPropertyExclusions );
		collectionBindings.forEach( StateManagementBindingPhase.CollectionMapping::bindCollection );
		oneToManyCollectionBindings.forEach( StateManagementBindingPhase.OneToManyAuditCollection::bindOneToManyAuditCollection );
	}

	@Override
	public void addMappedSuperclassPropertyHandoff(MappedSuperclassPropertyHandoff handoff) {
		mappedSuperclassPropertyHandoffs.put( handoff.property(), handoff );
	}

	@Override
	public MappedSuperclassPropertyHandoff getMappedSuperclassPropertyHandoff(Property property) {
		return mappedSuperclassPropertyHandoffs.get( property );
	}

	@Override
	public MappedSuperclassPropertyHandoff getMappedSuperclassPropertyHandoff(PersistentClass owner, Property property) {
		final MappedSuperclassPropertyHandoff handoff = getMappedSuperclassPropertyHandoff( property );
		return handoff != null && handoff.owner() == owner ? handoff : null;
	}

	@Override
	public List<MappedSuperclassPropertyHandoff> getMappedSuperclassPropertyHandoffs(
			MappedSuperclassContributionView contribution) {
		return mappedSuperclassPropertyHandoffs.values()
				.stream()
				.filter( handoff -> handoff.contribution().contribution() == contribution.contribution() )
				.toList();
	}

	@Override
	public List<MappedSuperclassPropertyHandoff> getMappedSuperclassPropertyHandoffs(PersistentClass owner) {
		return mappedSuperclassPropertyHandoffs.values()
				.stream()
				.filter( handoff -> handoff.owner() == owner )
				.toList();
	}

	@Override
	public void addNaturalIdPropertyHandoff(NaturalIdPropertyHandoff handoff) {
		naturalIdPropertyHandoffs.put( handoff.property(), handoff );
		naturalIdPropertyHandoffList.add( handoff );
	}

	@Override
	public NaturalIdPropertyHandoff getNaturalIdPropertyHandoff(Property property) {
		return naturalIdPropertyHandoffs.get( property );
	}

	@Override
	public List<NaturalIdPropertyHandoff> getNaturalIdPropertyHandoffs(NaturalIdContributionView contribution) {
		return naturalIdPropertyHandoffList
				.stream()
				.filter( handoff -> handoff.contribution().contribution() == contribution.contribution() )
				.toList();
	}

	@Override
	public void addCollationPropertyHandoff(CollationPropertyHandoff handoff) {
		collationPropertyHandoffs.put( handoff.property(), handoff );
		collationPropertyHandoffList.add( handoff );
	}

	@Override
	public CollationPropertyHandoff getCollationPropertyHandoff(Property property) {
		return collationPropertyHandoffs.get( property );
	}

	@Override
	public List<CollationPropertyHandoff> getCollationPropertyHandoffs(CollationContributionView contribution) {
		return collationPropertyHandoffList
				.stream()
				.filter( handoff -> handoff.contribution().contribution() == contribution.contribution() )
				.toList();
	}

	@Override
	public void addTenantIdPropertyHandoff(TenantIdPropertyHandoff handoff) {
		tenantIdPropertyHandoffsByRoot.put( handoff.rootClass(), handoff );
		tenantIdPropertyHandoffsByProperty.put( handoff.property(), handoff );
		tenantIdPropertyHandoffList.add( handoff );
	}

	@Override
	public TenantIdPropertyHandoff getTenantIdPropertyHandoff(RootClass rootClass) {
		return tenantIdPropertyHandoffsByRoot.get( rootClass );
	}

	@Override
	public TenantIdPropertyHandoff getTenantIdPropertyHandoff(Property property) {
		return tenantIdPropertyHandoffsByProperty.get( property );
	}

	@Override
	public TenantIdPropertyHandoff getTenantIdPropertyHandoff(TenantIdBindingView binding) {
		return tenantIdPropertyHandoffList
				.stream()
				.filter( handoff -> handoff.binding().binding() == binding.binding() )
				.findFirst()
				.orElse( null );
	}

	@Override
	public void addVersionPropertyHandoff(VersionPropertyHandoff handoff) {
		versionPropertyHandoffsByRoot.put( handoff.rootClass(), handoff );
		versionPropertyHandoffsByProperty.put( handoff.property(), handoff );
		versionPropertyHandoffList.add( handoff );
	}

	@Override
	public VersionPropertyHandoff getVersionPropertyHandoff(RootClass rootClass) {
		return versionPropertyHandoffsByRoot.get( rootClass );
	}

	@Override
	public VersionPropertyHandoff getVersionPropertyHandoff(Property property) {
		return versionPropertyHandoffsByProperty.get( property );
	}

	@Override
	public VersionPropertyHandoff getVersionPropertyHandoff(VersionBindingView binding) {
		return versionPropertyHandoffList
				.stream()
				.filter( handoff -> handoff.binding().binding() == binding.binding() )
				.findFirst()
				.orElse( null );
	}

	@Override
	public void addEmbeddableComponentHandoff(EmbeddableComponentHandoff handoff) {
		embeddableComponentHandoffs.put( handoff.component(), handoff );
		embeddableComponentHandoffList.add( handoff );
		bootBindingModel.addEmbeddableComponentHandoff( handoff.contribution(), handoff.component() );
	}

	@Override
	public EmbeddableComponentHandoff getEmbeddableComponentHandoff(Component component) {
		return embeddableComponentHandoffs.get( component );
	}

	@Override
	public List<EmbeddableComponentHandoff> getEmbeddableComponentHandoffs(
			EmbeddableContributionView contribution) {
		return embeddableComponentHandoffList
				.stream()
				.filter( handoff -> handoff.contribution().contribution() == contribution.contribution() )
				.toList();
	}

	@Override
	public List<EmbeddableComponentHandoff> getEmbeddableComponentHandoffs(PersistentClass owner) {
		return embeddableComponentHandoffList
				.stream()
				.filter( handoff -> handoff.owner() == owner )
				.toList();
	}

	@Override
	public void addStateManagementCollectionBinding(StateManagementBindingPhase.CollectionMapping binding) {
		stateManagementCollectionBindings.add( binding );
	}

	@Override
	public void addStateManagementOneToManyCollectionBinding(StateManagementBindingPhase.OneToManyAuditCollection binding) {
		stateManagementOneToManyCollectionBindings.add( binding );
	}

	@Override
	public void addStateManagementFinalizer(StateManagementBindingPhase.Finalizer binding) {
		stateManagementFinalizers.add( binding );
	}

	@Override
	public void runStateManagementFinalizers() {
		final var bindings = List.copyOf( stateManagementFinalizers );
		stateManagementFinalizers.clear();
		bindings.forEach( StateManagementBindingPhase.Finalizer::finalizeStateManagement );
	}

	private String resolveSchemaName(Identifier explicit) {
		if ( explicit != null ) {
			return explicit.getCanonicalName();
		}

		var defaultNamespace = metadataBuildingContext.getMetadataCollector()
				.getDatabase()
				.getDefaultNamespace();
		if ( defaultNamespace != null ) {
			final Identifier defaultSchemaName = defaultNamespace.getName().getSchema();
			if ( defaultSchemaName != null ) {
				return defaultSchemaName.getCanonicalName();
			}
		}
		return null;
	}

	private String resolveCatalogName(Identifier explicit) {
		if ( explicit != null ) {
			return explicit.getCanonicalName();
		}

		var defaultNamespace = metadataBuildingContext.getMetadataCollector()
				.getDatabase()
				.getDefaultNamespace();
		if ( defaultNamespace != null ) {
			final Identifier defaultCatalogName = defaultNamespace.getName().getCatalog();
			if ( defaultCatalogName != null ) {
				return defaultCatalogName.getCanonicalName();
			}
		}
		return null;

	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Filter def

	@Override
	public void apply(FilterDefRegistration registration) {
		addFilterDefinition( new FilterDefinition(
				registration.name(),
				registration.defaultCondition(),
				registration.autoEnabled(),
				registration.applyToLoadByKey(),
				extractParameterMap( registration ),
				extractParameterResolvers( registration )
		) );
	}

	private Map<String, JdbcMapping> extractParameterMap(FilterDefRegistration registration) {
		final Map<String, ClassDetails> parameters = registration.parameterTypes();
		if ( CollectionHelper.isEmpty( parameters ) ) {
			return Collections.emptyMap();
		}

		final Map<String, JdbcMapping> result = new HashMap<>();
		parameters.forEach( (name, typeDetails) -> {
			result.put( name, FilterDefBinder.resolveFilterParamType( typeDetails.toJavaClass(), metadataBuildingContext ) );
		} );
		return result;
	}

	private Map<String, ManagedBean<? extends Supplier<?>>> extractParameterResolvers(FilterDefRegistration registration) {
		final Map<String, ClassDetails> parameterResolvers = registration.parameterResolvers();
		if ( CollectionHelper.isEmpty( parameterResolvers ) ) {
			return Collections.emptyMap();
		}

		final ManagedBeanRegistry managedBeanRegistry = metadataBuildingContext.getManagedBeanRegistry();
		final Map<String, ManagedBean<? extends Supplier<?>>> result = new HashMap<>();
		parameterResolvers.forEach( (name, resolver) -> {
			result.put( name, managedBeanRegistry.getBean( resolver.toJavaClass() ) );
		} );
		return result;
	}

}
