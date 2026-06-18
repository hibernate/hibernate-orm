/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.models.bind.internal.model.BootBindingModel;
import org.hibernate.boot.models.bind.internal.binders.AssociationTableBinding;
import org.hibernate.boot.models.bind.internal.binders.AssociationIdentifierBinding;
import org.hibernate.boot.models.bind.internal.binders.AssociationTargetBinding;
import org.hibernate.boot.models.bind.internal.binders.CollectionTableBinding;
import org.hibernate.boot.models.bind.internal.binders.DerivedIdentifierBinding;
import org.hibernate.boot.models.bind.internal.binders.EntityTypeBinder;
import org.hibernate.boot.models.bind.internal.binders.ForeignKeyBinding;
import org.hibernate.boot.models.bind.internal.binders.IdentifierBinding;
import org.hibernate.boot.models.bind.internal.binders.IdentifiableTypeBinder;
import org.hibernate.boot.models.bind.internal.binders.InversePluralAssociationBinding;
import org.hibernate.boot.models.bind.internal.binders.InverseToOneAssociationBinding;
import org.hibernate.boot.models.bind.internal.binders.ManagedTypeBinder;
import org.hibernate.boot.models.bind.internal.binders.MappedSuperTypeBinder;
import org.hibernate.boot.models.bind.internal.binders.PropertyMapKeyBinding;
import org.hibernate.boot.models.bind.internal.binders.TableForeignKeyBinding;
import org.hibernate.boot.models.bind.spi.BindingState;
import org.hibernate.boot.models.bind.spi.MetadataCollector;
import org.hibernate.boot.models.bind.spi.TableOwner;
import org.hibernate.boot.models.bind.spi.TableReference;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.boot.models.categorize.spi.FilterDefRegistration;
import org.hibernate.boot.models.categorize.spi.IdentifiableTypeMetadata;
import org.hibernate.boot.models.categorize.spi.ManagedTypeMetadata;
import org.hibernate.boot.query.NamedResultSetMappingDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.internal.util.KeyedConsumer;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.models.spi.ClassDetails;
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
	private final MetadataBuildingContext metadataBuildingContext;
	private final MetadataCollector metadataCollector;
	private final BootBindingModel bootBindingModel = new BootBindingModel();

	private final Database database;
	private final JdbcServices jdbcServices;

	private final Map<String, TableReference> tableMap = new HashMap<>();
	private final Map<TableOwner, TableReference> tableByOwnerMap = new HashMap<>();
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

	private final Map<ClassDetails, ManagedTypeBinder> typeBinders = new HashMap<>();
	private final Map<ClassDetails, IdentifiableTypeBinder> typeBindersBySuper = new HashMap<>();
	private final Map<EntityTypeMetadata, IdentifierBinding> identifierBindings = new HashMap<>();

	public BindingStateImpl(MetadataBuildingContext metadataBuildingContext, MetadataCollector metadataCollector) {
		this.metadataBuildingContext = metadataBuildingContext;
		this.metadataCollector = metadataCollector;
		this.database = metadataBuildingContext.getMetadataCollector().getDatabase();
		this.jdbcServices = metadataBuildingContext.getBootstrapContext().getServiceRegistry().getService( JdbcServices.class );
	}

	@Override
	public MetadataBuildingContext getMetadataBuildingContext() {
		return metadataBuildingContext;
	}

	@Override
	public Database getDatabase() {
		return database;
	}

	@Override
	public JdbcServices getJdbcServices() {
		return jdbcServices;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return metadataBuildingContext.getBootstrapContext().getTypeConfiguration();
	}

	@Override
	public BootBindingModel getBootBindingModel() {
		return bootBindingModel;
	}

	@Override
	public void addEntityBinding(PersistentClass entityBinding) {
		metadataCollector.addEntityBinding( entityBinding );
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
	public void addUniquePropertyReference(String referencedEntityName, String referencedPropertyName) {
		metadataCollector.addUniquePropertyReference( referencedEntityName, referencedPropertyName );
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
	public void registerCompositeUserType(Class<?> embeddableClass, Class<? extends CompositeUserType<?>> userTypeClass) {
		metadataCollector.registerCompositeUserType( embeddableClass, userTypeClass );
	}

	@Override
	public void addCollectionTypeRegistration(
			CollectionClassification classification,
			Class<? extends UserCollectionType> userTypeClass,
			Map<String, String> parameters) {
		metadataCollector.addCollectionTypeRegistration( classification, userTypeClass, parameters );
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
	public void addIdentifierBinding(EntityTypeMetadata rootType, IdentifierBinding identifierBinding) {
		identifierBindings.put( rootType, identifierBinding );
	}

	@Override
	public IdentifierBinding getIdentifierBinding(EntityTypeMetadata rootType) {
		return identifierBindings.get( rootType );
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
	public void addTable(TableOwner owner, TableReference table) {
		tableMap.put( table.logicalName().getCanonicalName(), table );
		tableByOwnerMap.put( owner, table );
	}

	@Override
	public void addSecondaryTable(SecondaryTable table) {
		tableMap.put( table.logicalName().getCanonicalName(), table );
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
				extractParameterMap( registration )
		) );
	}

	private Map<String, JdbcMapping> extractParameterMap(FilterDefRegistration registration) {
		final Map<String, ClassDetails> parameters = registration.parameters();
		if ( CollectionHelper.isEmpty( parameters ) ) {
			return Collections.emptyMap();
		}

		final TypeConfiguration typeConfiguration = getTypeConfiguration();
		final Map<String, JdbcMapping> result = new HashMap<>();
		parameters.forEach( (name, typeDetails) -> {
			result.put( name, typeConfiguration.getBasicTypeForJavaType( typeDetails.toJavaClass() ) );
		} );
		return result;
	}

}
