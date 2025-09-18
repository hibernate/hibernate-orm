/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot;

import org.hibernate.Internal;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.internal.log.SubSystemLogging;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;
import org.jboss.logging.annotations.ValidIdRanges;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.TRACE;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Logging related to Hibernate bootstrapping
 */
@SubSystemLogging(
		name = BootLogging.NAME,
		description = "Logging related to bootstrapping of a SessionFactory / EntityManagerFactory"
)
@MessageLogger(projectCode = "HHH")
@ValidIdRanges({
	@ValidIdRange(min = 160101, max = 160200),
	@ValidIdRange(min = 160201, max = 160260)
})
@Internal
public interface BootLogging extends BasicLogger {
	String NAME = SubSystemLogging.BASE + ".boot";
	BootLogging BOOT_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), BootLogging.class, NAME );

	@LogMessage(level = WARN)
	@Message(id = 160101, value = "Duplicate generator name %s")
	void duplicateGeneratorName(String name);


	@LogMessage(level = DEBUG)
	@Message(id = 160111, value = "Package not found or no package-info.java: %s")
	void packageNotFound(String packageName);

	@LogMessage(level = WARN)
	@Message(id = 160112, value = "LinkageError while attempting to load package: %s")
	void linkageError(String packageName, @Cause LinkageError e);

	@LogMessage(level = TRACE)
	@Message(id = 160121, value = "Trying via [new URL(\"%s\")]")
	void tryingURL(String name);

	@LogMessage(level = TRACE)
	@Message(id = 160122, value = "Trying via [ClassLoader.getResourceAsStream(\"%s\")]")
	void tryingClassLoader(String name);

	@LogMessage(level = WARN)
	@Message(id = 160130, value = "Ignoring unique constraints specified on table generator [%s]")
	void ignoringTableGeneratorConstraints(String name);

	@LogMessage(level = WARN)
	@Message(
			id = 160131,
			value = """
					@Convert annotation applied to Map attribute [%s] did not explicitly specify\
					'attributeName="key" or 'attributeName="value"' as required by spec;\
					attempting to infer whether converter applies to key or value"""
	)
	void nonCompliantMapConversion(String collectionRole);

	@LogMessage(level = WARN)
	@Message(
			id = 160133,
			value = """
					'%1$s.%2$s' uses both @NotFound and FetchType.LAZY;\
					@ManyToOne and @OneToOne associations mapped with @NotFound are forced to EAGER fetching""")
	void ignoreNotFoundWithFetchTypeLazy(String entity, String association);

	// --- New typed TRACE/DEBUG messages for boot internals ---
	@LogMessage(level = TRACE)
	@Message(id = 160140, value = "Binding formula: %s")
	void bindingFormula(String formula);

	@LogMessage(level = TRACE)
	@Message(id = 160141, value = "Binding column: %s")
	void bindingColumn(String column);

	@LogMessage(level = TRACE)
	@Message(id = 160142, value = "Column mapping overridden for property: %s")
	void columnMappingOverridden(String propertyName);

	@LogMessage(level = TRACE)
	@Message(id = 160143, value = "Could not perform @ColumnDefault lookup as 'PropertyData' did not give access to XProperty")
	void couldNotPerformColumnDefaultLookup();

	@LogMessage(level = TRACE)
	@Message(id = 160144, value = "Could not perform @GeneratedColumn lookup as 'PropertyData' did not give access to XProperty")
	void couldNotPerformGeneratedColumnLookup();

	@LogMessage(level = TRACE)
	@Message(id = 160145, value = "Could not perform @Check lookup as 'PropertyData' did not give access to XProperty")
	void couldNotPerformCheckLookup();

	@LogMessage(level = TRACE)
	@Message(id = 160146, value = "Binding embeddable with path: %s")
	void bindingEmbeddable(String path);

	@LogMessage(level = TRACE)
	@Message(id = 160147, value = "Binding filter definition: %s")
	void bindingFilterDefinition(String name);

	@LogMessage(level = TRACE)
	@Message(id = 160148, value = "Second pass for collection: %s")
	void secondPassForCollection(String role);

	@LogMessage(level = TRACE)
	@Message(id = 160149, value = "Binding collection role: %s")
	void bindingCollectionRole(String role);

	@LogMessage(level = TRACE)
	@Message(id = 160150, value = "Binding one-to-many association through foreign key: %s")
	void bindingOneToManyThroughForeignKey(String role);

	@LogMessage(level = TRACE)
	@Message(id = 160151, value = "Binding one-to-many association through association table: %s")
	void bindingOneToManyThroughAssociationTable(String role);

	@LogMessage(level = TRACE)
	@Message(id = 160152, value = "Binding many-to-many association through association table: %s")
	void bindingManyToManyThroughAssociationTable(String role);

	@LogMessage(level = TRACE)
	@Message(id = 160153, value = "Binding many-to-any: %s")
	void bindingManyToAny(String role);

	@LogMessage(level = TRACE)
	@Message(id = 160154, value = "Binding element collection to collection table: %s")
	void bindingElementCollectionToCollectionTable(String role);

	@LogMessage(level = TRACE)
	@Message(id = 160155, value = "Import: %s -> %s")
	void importEntry(String importName, String className);

	@LogMessage(level = TRACE)
	@Message(id = 160156, value = "Processing association property references")
	void processingAssociationPropertyReferences();

	@LogMessage(level = TRACE)
	@Message(id = 160157, value = "Mapping class: %s -> %s")
	void mappingClassToTable(String entityName, String tableName);

	@LogMessage(level = TRACE)
	@Message(id = 160158, value = "Mapping joined-subclass: %s -> %s")
	void mappingJoinedSubclassToTable(String entityName, String tableName);

	@LogMessage(level = TRACE)
	@Message(id = 160159, value = "Mapping union-subclass: %s -> %s")
	void mappingUnionSubclassToTable(String entityName, String tableName);

	@LogMessage(level = TRACE)
	@Message(id = 160160, value = "Mapped property: %s -> [%s]")
	void mappedProperty(String propertyName, String columns);

	@LogMessage(level = TRACE)
	@Message(id = 160161, value = "Binding dynamic component [%s]")
	void bindingDynamicComponent(String role);

	@LogMessage(level = TRACE)
	@Message(id = 160162, value = "Binding virtual component [%s] to owner class [%s]")
	void bindingVirtualComponentToOwner(String role, String ownerClassName);

	@LogMessage(level = TRACE)
	@Message(id = 160163, value = "Binding virtual component [%s] as dynamic")
	void bindingVirtualComponentAsDynamic(String role);

	@LogMessage(level = TRACE)
	@Message(id = 160164, value = "Binding component [%s]")
	void bindingComponent(String role);

	@LogMessage(level = TRACE)
	@Message(id = 160165, value = "Attempting to determine component class by reflection %s")
	void attemptingToDetermineComponentClassByReflection(String role);

	@LogMessage(level = TRACE)
	@Message(id = 160166, value = "Mapped collection: %s")
	void mappedCollection(String role);

	@LogMessage(level = TRACE)
	@Message(id = 160167, value = "Mapping collection: %s -> %s")
	void mappingCollectionToTable(String role, String tableName);

	@LogMessage(level = TRACE)
	@Message(id = 160168, value = "Binding natural id UniqueKey for entity: %s")
	void bindingNaturalIdUniqueKey(String entityName);

	@LogMessage(level = TRACE)
	@Message(id = 160169, value = "Binding named query '%s' to [%s]")
	void bindingNamedQuery(String queryName, String bindingTarget);

	@LogMessage(level = TRACE)
	@Message(id = 160170, value = "Binding named native query '%s' to [%s]")
	void bindingNamedNativeQuery(String queryName, String bindingTarget);

	@LogMessage(level = TRACE)
	@Message(id = 160171, value = "Binding SQL result set mapping '%s' to [%s]")
	void bindingSqlResultSetMapping(String name, String target);

	@LogMessage(level = TRACE)
	@Message(id = 160172, value = "Bound named stored procedure query: %s => %s")
	void boundStoredProcedureQuery(String name, String procedure);

	@LogMessage(level = TRACE)
	@Message(id = 160173, value = "Adding global sequence generator with name: %s")
	void addingGlobalSequenceGenerator(String name);

	@LogMessage(level = TRACE)
	@Message(id = 160174, value = "Adding global table generator with name: %s")
	void addingGlobalTableGenerator(String name);

	@LogMessage(level = TRACE)
	@Message(id = 160175, value = "Binding entity with annotated class: %s")
	void bindingEntityWithAnnotatedClass(String className);

	@LogMessage(level = DEBUG)
	@Message(id = 160176, value = "Import name [%s] overrode previous [{%s}]")
	void importOverrodePrevious(String importName, String previous);

	@LogMessage(level = TRACE)
	@Message(id = 160177, value = "%s")
	void mappedCollectionDetails(String details);

	@LogMessage(level = TRACE)
	@Message(id = 160178, value = "Mapping entity secondary table: %s -> %s")
	void mappingEntitySecondaryTableToTable(String entityName, String tableName);

	@LogMessage(level = DEBUG)
	@Message(id = 160180, value = "Unexpected ServiceRegistry type [%s] encountered during building of MetadataSources; may cause problems later attempting to construct MetadataBuilder")
	void unexpectedServiceRegistryType(String registryType);

	@LogMessage(level = TRACE)
	@Message(id = 160181, value = "Created database namespace [logicalName=%s, physicalName=%s]")
	void createdDatabaseNamespace(Namespace.Name logicalName, Namespace.Name physicalName);

	@LogMessage(level = DEBUG)
	@Message(id = 160182, value = "Could load component class [%s]")
	void couldLoadComponentClass(String className, @Cause Throwable ex);

	@LogMessage(level = DEBUG)
	@Message(id = 160183, value = "Unable to load explicit any-discriminator type name as Java Class - %s")
	void unableToLoadExplicitAnyDiscriminatorType(String typeName);

	@LogMessage(level = DEBUG)
	@Message(id = 160184, value = "Ignoring exception thrown when trying to build IdentifierGenerator as part of Metadata building")
	void ignoringExceptionBuildingIdentifierGenerator(@Cause Throwable ex);

	@LogMessage(level = TRACE)
	@Message(id = 160185, value = "Binding component [%s] to explicitly specified class [%s]")
	void bindingComponentToExplicitClass(String role, String className);

	@LogMessage(level = DEBUG)
	@Message(id = 160186, value = "Unable to determine component class name via reflection, and explicit class name not given; role=[%s]")
	void unableToDetermineComponentClassByReflection(String role);

	@LogMessage(level = DEBUG)
	@Message(id = 160187, value = "Replacing Table registration(%s) : %s -> %s")
	void replacingTableRegistration(String logicalName, String previous, String table);

	@LogMessage(level = DEBUG)
	@Message(id = 160188, value = "Ignoring %s XML mappings due to '%s'")
	void ignoringXmlMappings(int count, String setting);

	@LogMessage(level = WARN)
	@Message(id = 160189, value = "Duplicate fetch profile name '%s'")
	void duplicatedFetchProfile(String name);

	// EntityBinder discriminator handling

	@LogMessage(level = TRACE)
	@Message(id = 160190, value = "Ignoring explicit @DiscriminatorColumn annotation on: %s")
	void ignoringExplicitDiscriminatorForJoined(String className);

	@LogMessage(level = TRACE)
	@Message(id = 160191, value = "Inferring implicit @DiscriminatorColumn using defaults for: %s")
	void inferringImplicitDiscriminatorForJoined(String className);

	// GeneratorBinder

	@LogMessage(level = TRACE)
	@Message(id = 160192, value = "Added generator with name: %s, strategy: %s")
	void addedGenerator(String name, String strategy);

	@LogMessage(level = TRACE)
	@Message(id = 160193, value = "Added sequence generator with name: %s")
	void addedSequenceGenerator(String name);

	@LogMessage(level = TRACE)
	@Message(id = 160194, value = "Added table generator with name: %s")
	void addedTableGenerator(String name);

	@LogMessage(level = DEBUG)
	@Message(id = 160195,
			value = """
					ServiceRegistry passed to MetadataBuilder was a BootstrapServiceRegistry; \
					this likely won't end well if attempt is made to build SessionFactory""")
	void badServiceRegistry();

	@LogMessage(level = DEBUG)
	@Message(id = 160201, value = "Unable to close cfg.xml resource stream")
	void unableToCloseCfgXmlResourceStream(@Cause Throwable e);

	@LogMessage(level = DEBUG)
	@Message(id = 160202, value = "Unable to close cfg.xml URL stream")
	void unableToCloseCfgXmlUrlStream(@Cause Throwable e);

	@LogMessage(level = DEBUG)
	@Message(id = 160203, value = "Unable to close properties file stream [%s]")
	void unableToClosePropertiesFileStream(String path, @Cause Throwable e);

	@LogMessage(level = DEBUG)
	@Message(id = 160204, value = "cfg.xml document did not define namespaces; wrapping in custom event reader to introduce namespace information")
	void cfgXmlDocumentDidNotDefineNamespaces();

	@LogMessage(level = DEBUG)
	@Message(id = 160212, value = "Unable to adjust relative <jar-file/> URL [%s] relative to root URL [%s]")
	void unableToAdjustRelativeJarFileUrl(String filePart, String rootPath, @Cause Throwable e);

	@LogMessage(level = TRACE)
	@Message(id = 160213, value = "JAR URL from URL Entry: %s >> %s")
	void jarUrlFromUrlEntry(String url, String jarUrl);

	@LogMessage(level = TRACE)
	@Message(id = 160217, value = "Registering AttributeConverter '%s'")
	void registeringAttributeConverter(String converter);

	@LogMessage(level = DEBUG)
	@Message(id = 160218, value = "Skipping registration of discovered AttributeConverter '%s' for auto-apply")
	void skippingRegistrationAttributeConverterForAutoApply(String converter);

	@LogMessage(level = DEBUG)
	@Message(id = 160219, value = "Skipping duplicate '@ConverterRegistration' for '%s'")
	void skippingDuplicateConverterRegistration(String converter);

	@LogMessage(level = DEBUG)
	@Message(id = 160220, value = "Removed potentially auto-applicable converter '%s' due to @ConverterRegistration")
	void removedPotentiallyAutoApplicableConverterDueToRegistration(String converter);

	@LogMessage(level = DEBUG)
	@Message(id = 160221, value = "Overwrote existing registration [%s] for type definition.")
	void overwroteExistingRegistrationForTypeDefinition(String name);

	@LogMessage(level = TRACE)
	@Message(id = 160222, value = "Injecting JPA temp ClassLoader [%s] into BootstrapContext; was [%s]")
	void injectingJpaTempClassLoader(Object newLoader, Object oldLoader);

	@LogMessage(level = TRACE)
	@Message(id = 160223, value = "Injecting ScanOptions [%s] into BootstrapContext; was [%s]")
	void injectingScanOptions(Object newOptions, Object oldOptions);

	@LogMessage(level = TRACE)
	@Message(id = 160224, value = "Injecting ScanEnvironment [%s] into BootstrapContext; was [%s]")
	void injectingScanEnvironment(Object newEnv, Object oldEnv);

	@LogMessage(level = TRACE)
	@Message(id = 160225, value = "Injecting Scanner [%s] into BootstrapContext; was [%s]")
	void injectingScanner(Object newScanner, Object oldScanner);

	@LogMessage(level = TRACE)
	@Message(id = 160226, value = "Injecting ArchiveDescriptorFactory [%s] into BootstrapContext; was [%s]")
	void injectingArchiveDescriptorFactory(Object newFactory, Object oldFactory);

	// Strategy selector

	@LogMessage(level = TRACE)
	@Message(id = 160227, value = "Strategy selector for %s: '%s' -> %s")
	void strategySelectorMapping(String strategySimpleName, String name, String implementationName);

	@LogMessage(level = TRACE)
	@Message(id = 160228, value = "Strategy selector for %s: '%s' -> %s (replacing %s)")
	void strategySelectorMappingReplacing(String strategySimpleName, String name, String implementationName, String oldImplementationName);

	@LogMessage(level = DEBUG)
	@Message(id = 160229, value = "Named strategy map did not exist on call to unregister")
	void namedStrategyMapDidNotExistOnUnregister();

	// LoadedConfig

	@LogMessage(level = DEBUG)
	@Message(id = 160230, value = "Listener [%s] defined as part of a group also defined event type")
	void listenerDefinedAlsoDefinedEventType(String listenerClass);

	@LogMessage(level = DEBUG)
	@Message(id = 160231, value = "More than one cfg.xml file attempted to supply SessionFactory name: [%s], [%s].  Keeping initially discovered one [%s]")
	void moreThanOneCfgXmlSuppliedSessionFactoryName(String first, String second, String keeping);

	// HBM binding

	@LogMessage(level = TRACE)
	@Message(id = 160233, value = "Creating FetchProfile: %s")
	void creatingFetchProfile(String name);

	@LogMessage(level = TRACE)
	@Message(id = 160234, value = "Processing <identifier-generator/> : %s")
	void processingIdentifierGenerator(String name);

	@LogMessage(level = TRACE)
	@Message(id = 160235, value = "Processed type definition : %s -> %s")
	void processedTypeDefinition(String name, String impl);

	@LogMessage(level = TRACE)
	@Message(id = 160236, value = "No temp ClassLoader provided; using live ClassLoader to load potentially unsafe class: %s")
	void noTempClassLoaderProvidedUsingLiveClassLoader(String className);

	@LogMessage(level = TRACE)
	@Message(id = 160237, value = "Registering non-interface strategy : %s")
	void registeringNonInterfaceStrategy(String roleName);

	@LogMessage(level = TRACE)
	@Message(id = 160238, value = "Indexing mapping document [%s] for purpose of building entity hierarchy ordering")
	void indexingMappingDocumentForHierarchyOrdering(String mappingDocumentName);

	@LogMessage(level = DEBUG)
	@Message(id = 160239, value = "Applying filter definition with condition: %s")
	void applyingFilterDefinitionCondition(String condition);

	@LogMessage(level = TRACE)
	@Message(id = 160240, value = "Processed filter definition: %s")
	void processedFilterDefinition(String name);

	@LogMessage(level = DEBUG)
	@Message(id = 160241, value = "Entity supertype named as extends [%s] for subclass [%s:%s] not found")
	void entitySupertypeExtendsNotFound(String extendsName, Origin origin, String subclassName);

	@LogMessage(level = DEBUG)
	@Message(id = 160242, value = "filter-def [name=%s, origin=%s] defined multiple conditions, accepting arbitrary one")
	void filterDefDefinedMultipleConditions(String name, String origin);

	@LogMessage(level = TRACE)
	@Message(id = 160243, value = "Checking auto-apply AttributeConverter [%s] (domain-type=%s) for match against %s : %s.%s (type=%s)")
	void checkingAutoApplyAttributeConverter(String converterClass, String domainTypeSignature, String siteDescriptor, String declaringType, String memberName, String memberTypeName);

	@LogMessage(level = DEBUG)
	@Message(id = 160244, value = "Skipping HBM processing of entity hierarchy [%s], as at least one entity [%s] has been processed")
	void skippingHbmProcessingOfEntityHierarchy(String rootEntityName, String processedEntity);
}
