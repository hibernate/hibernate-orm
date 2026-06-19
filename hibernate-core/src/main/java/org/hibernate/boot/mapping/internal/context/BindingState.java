/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.context;

import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.mapping.internal.relational.SecondaryTable;
import org.hibernate.boot.mapping.internal.relational.TableOwner;
import org.hibernate.boot.mapping.internal.relational.TableReference;
import org.hibernate.boot.mapping.internal.model.BootBindingModel;
import org.hibernate.boot.mapping.internal.model.IdentifierContribution;
import org.hibernate.boot.mapping.internal.binders.AssociationTargetBinding;
import org.hibernate.boot.mapping.internal.binders.AssociationIdentifierBinding;
import org.hibernate.boot.mapping.internal.binders.AssociationTableBinding;
import org.hibernate.boot.mapping.internal.binders.CollectionTableBinding;
import org.hibernate.boot.mapping.internal.binders.DerivedIdentifierBinding;
import org.hibernate.boot.mapping.internal.binders.ForeignKeyBinding;
import org.hibernate.boot.mapping.internal.binders.IdentifierBinding;
import org.hibernate.boot.mapping.internal.binders.IdentifiableTypeBinder;
import org.hibernate.boot.mapping.internal.binders.InversePluralAssociationBinding;
import org.hibernate.boot.mapping.internal.binders.InverseToOneAssociationBinding;
import org.hibernate.boot.mapping.internal.binders.ManagedTypeBinder;
import org.hibernate.boot.mapping.internal.binders.PropertyMapKeyBinding;
import org.hibernate.boot.mapping.internal.binders.TableForeignKeyBinding;
import org.hibernate.boot.mapping.internal.view.IdentifierContributionView;
import org.hibernate.boot.mapping.internal.categorize.EntityTypeMetadata;
import org.hibernate.boot.mapping.internal.categorize.FilterDefRegistration;
import org.hibernate.boot.mapping.internal.categorize.ManagedTypeMetadata;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.internal.util.KeyedConsumer;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.boot.query.NamedResultSetMappingDescriptor;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserCollectionType;
import org.hibernate.usertype.UserType;

import jakarta.persistence.AttributeConverter;

/// Mutable working state shared by binders while producing Hibernate's boot-time
/// mapping model.
///
/// This is intentionally distinct from [MetadataCollector].  `BindingState` owns
/// phase-local state needed while binding is in progress, while `MetadataCollector`
/// owns completed metadata contributions.
///
/// @since 9.0
/// @author Steve Ebersole
public interface BindingState {
	/// Metadata building context for the current boot run.
	MetadataBuildingContext getMetadataBuildingContext();

	/// Database model being populated during binding.
	default Database getDatabase() {
		return getMetadataBuildingContext().getMetadataCollector().getDatabase();
	}

	/// JDBC services used for dialect and identifier handling.
	JdbcServices getJdbcServices();

	/// Type configuration used while binding values and metadata registrations.
	TypeConfiguration getTypeConfiguration();

	/// Horizontal binding model populated from categorized source facts.
	BootBindingModel getBootBindingModel();

	/// Register an entity binding with the metadata collector.
	void addEntityBinding(PersistentClass entityBinding);

	/// Register a mapped-superclass binding for eventual publication to the metadata collector.
	void addMappedSuperclass(Class<?> mappedSuperclassClass, MappedSuperclass mappedSuperclass);

	/// Register a collection binding for eventual publication to the metadata collector.
	void addCollectionBinding(Collection collection);

	/// Register an entity-name import for eventual publication to the metadata collector.
	void addImport(String importName, String entityName);

	/// Register a unique property reference with the metadata collector.
	void addUniquePropertyReference(String referencedEntityName, String referencedPropertyName);

	/// Register an identifier generator for eventual publication to the metadata collector.
	void addIdentifierGenerator(IdentifierGeneratorDefinition identifierGeneratorDefinition);

	/// Register a named entity graph for eventual publication to the metadata collector.
	void addNamedEntityGraph(NamedEntityGraphDefinition namedEntityGraphDefinition);

	/// Register a named SQL result set mapping for eventual publication to the metadata collector.
	void addResultSetMapping(NamedResultSetMappingDescriptor resultSetMappingDescriptor);

	/// Register a fetch profile for eventual publication to the metadata collector.
	void addFetchProfile(FetchProfile fetchProfile);

	/// Resolve a fetch profile already published to, or pending for, the metadata collector.
	FetchProfile getFetchProfile(String name);

	/// Register an auxiliary database object for eventual publication to the metadata collector.
	void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject);

	/// Register an auto-apply converter for eventual publication to the metadata collector.
	void addAttributeConverter(Class<? extends AttributeConverter<?, ?>> converterClass);

	/// Register an explicit converter for eventual publication to the metadata collector.
	void addRegisteredConversion(RegisteredConversion registeredConversion);

	/// Register a Java type descriptor for eventual publication to the metadata collector.
	void addJavaTypeRegistration(Class<?> domainType, JavaType<?> descriptor);

	/// Register a JDBC type descriptor for eventual publication to the metadata collector.
	void addJdbcTypeRegistration(int code, JdbcType descriptor);

	/// Register a custom user type for eventual publication to the metadata collector.
	void registerUserType(Class<?> domainClass, Class<? extends UserType<?>> userTypeClass);

	/// Register a custom composite user type for eventual publication to the metadata collector.
	void registerCompositeUserType(Class<?> embeddableClass, Class<? extends CompositeUserType<?>> userTypeClass);

	/// Register a collection type for eventual publication to the metadata collector.
	void addCollectionTypeRegistration(
			CollectionClassification classification,
			Class<? extends UserCollectionType> userTypeClass,
			java.util.Map<String,String> parameters);

	/// Register an embeddable instantiator for eventual publication to the metadata collector.
	void registerEmbeddableInstantiator(
			Class<?> embeddableClass,
			Class<? extends EmbeddableInstantiator> instantiatorClass);

	/// Resolve a filter definition already published to, or pending for, the metadata collector.
	FilterDefinition getFilterDefinition(String name);

	/// Register a filter definition for eventual publication to the metadata collector.
	void addFilterDefinition(FilterDefinition filterDefinition);

	/// Apply a categorized global filter definition to the mapping model.
	void apply(FilterDefRegistration registration);

	/// Number of table references currently known to the binding state.
	int getTableCount();

	/// Visit each known table reference keyed by its binding-state name.
	void forEachTable(KeyedConsumer<String,TableReference> consumer);

	/// Resolve a table reference by binding-state name.
	<T extends TableReference> T getTableByName(String name);

	/// Resolve the table reference owned by the given model object.
	<T extends TableReference> T getTableByOwner(TableOwner owner);

	/// Register a table reference for the given owner.
	void addTable(TableOwner owner, TableReference table);

	/// Register a secondary table reference.
	void addSecondaryTable(SecondaryTable table);

	/// Resolve secondary-table state by its Hibernate table binding.
	SecondaryTable getSecondaryTable(org.hibernate.mapping.Table table);

	/// Register a Join that represents an association table.
	void addAssociationTableBinding(AssociationTableBinding associationTableBinding);

	/// Resolve association-table state for a Join, if the Join represents one.
	AssociationTableBinding getAssociationTableBinding(Join join);

	/// Register a collection table whose key is bound after member binding.
	void addCollectionTableBinding(CollectionTableBinding collectionTableBinding);

	/// Visit registered collection table bindings.
	void forEachCollectionTableBinding(java.util.function.Consumer<CollectionTableBinding> consumer);

	/// Register a property-derived map key to resolve after all members are bound.
	void addPropertyMapKeyBinding(PropertyMapKeyBinding propertyMapKeyBinding);

	/// Visit property-derived map keys waiting for collection-index binding.
	void forEachPropertyMapKeyBinding(java.util.function.Consumer<PropertyMapKeyBinding> consumer);

	/// Register an association-valued identifier attribute to resolve after identifiers.
	void addAssociationIdentifierBinding(AssociationIdentifierBinding associationIdentifierBinding);

	/// Visit association-valued identifier attributes waiting for late binding.
	void forEachAssociationIdentifierBinding(java.util.function.Consumer<AssociationIdentifierBinding> consumer);

	/// Register a non-primary-key association target to resolve after members are bound.
	void addAssociationTargetBinding(AssociationTargetBinding associationTargetBinding);

	/// Visit non-primary-key association targets waiting for late binding.
	void forEachAssociationTargetBinding(java.util.function.Consumer<AssociationTargetBinding> consumer);

	/// Register a derived identifier association to resolve after member binding.
	void addDerivedIdentifierBinding(DerivedIdentifierBinding derivedIdentifierBinding);

	/// Visit derived identifier associations waiting for late binding.
	void forEachDerivedIdentifierBinding(java.util.function.Consumer<DerivedIdentifierBinding> consumer);

	/// Register an inverse plural association to resolve after owning collection keys exist.
	void addInversePluralAssociationBinding(InversePluralAssociationBinding inversePluralAssociationBinding);

	/// Visit inverse plural association bindings waiting for owning-side resolution.
	void forEachInversePluralAssociationBinding(java.util.function.Consumer<InversePluralAssociationBinding> consumer);

	/// Register an inverse to-one association to resolve after all members exist.
	void addInverseToOneAssociationBinding(InverseToOneAssociationBinding inverseToOneAssociationBinding);

	/// Visit inverse to-one association bindings waiting for owning-side resolution.
	void forEachInverseToOneAssociationBinding(java.util.function.Consumer<InverseToOneAssociationBinding> consumer);

	/// Register a foreign-key constraint to create after table keys are bound.
	void addForeignKeyBinding(ForeignKeyBinding foreignKeyBinding);

	/// Visit foreign-key constraints waiting for late binding.
	void forEachForeignKeyBinding(java.util.function.Consumer<ForeignKeyBinding> consumer);

	/// Register a table-key foreign-key constraint to create after table keys are bound.
	void addTableForeignKeyBinding(TableForeignKeyBinding tableForeignKeyBinding);

	/// Visit table-key foreign-key constraints waiting for late binding.
	void forEachTableForeignKeyBinding(java.util.function.Consumer<TableForeignKeyBinding> consumer);

	/// Register the identifier binding produced for an entity hierarchy root.
	void addIdentifierBinding(EntityTypeMetadata rootType, IdentifierBinding identifierBinding);

	/// Resolve the identifier binding for an entity hierarchy root.
	IdentifierBinding getIdentifierBinding(EntityTypeMetadata rootType);

	/// Register semantic identifier contribution state for an entity root.
	default void addIdentifierContribution(EntityTypeMetadata rootType, IdentifierContribution identifierContribution) {
		getBootBindingModel().addIdentifierContribution( rootType, identifierContribution );
	}

	/// Resolve semantic identifier contribution state for an entity root.
	default IdentifierContribution getIdentifierContribution(EntityTypeMetadata rootType) {
		return getBootBindingModel().getIdentifierContribution( rootType );
	}

	/// Resolve the finalized identifier contribution view for an entity root.
	default IdentifierContributionView getIdentifierContributionView(EntityTypeMetadata rootType) {
		return getBootBindingModel().getIdentifierContributionView( rootType );
	}


	/// Register the binder responsible for a categorized managed type.
	void registerTypeBinder(ManagedTypeMetadata type, ManagedTypeBinder binder);

	/// Resolve the binder responsible for a categorized managed type.
	default ManagedTypeBinder getTypeBinder(ManagedTypeMetadata type) {
		return getTypeBinder( type.getClassDetails() );
	}

	/// Resolve the binder responsible for the given managed class.
	ManagedTypeBinder getTypeBinder(ClassDetails type);

	/// Resolve the identifiable-type binder for the super type of the given class.
	IdentifiableTypeBinder getSuperTypeBinder(ClassDetails type);

	/// Visit each registered managed-type binder keyed by type name.
	void forEachType(KeyedConsumer<String,ManagedTypeBinder> consumer);

}
