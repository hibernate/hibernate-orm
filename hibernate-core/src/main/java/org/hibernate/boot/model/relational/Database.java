/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational;

import org.hibernate.MappingException;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.boot.model.naming.internal.PhysicalNamingStrategyHelper;

import java.io.Serializable;

import org.hibernate.boot.model.relational.internal.PhysicalNamespaceSnapshot;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jakarta.annotation.Nullable;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.relational.internal.PersistenceUnitJdbcEnvironment;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.UnknownServiceException;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Collections.emptyList;
import static org.hibernate.cfg.MappingSettings.GLOBALLY_QUOTED_IDENTIFIERS_SKIP_COLUMN_DEFINITIONS;
import static org.hibernate.engine.config.spi.StandardConverters.BOOLEAN;

/**
 * @author Steve Ebersole
 */
public class Database implements Serializable {

	private transient Dialect dialect;
	private transient TypeConfiguration typeConfiguration;
	private transient JdbcEnvironment jdbcEnvironment;
	private final Map<Namespace.LogicalNamespaceName,Namespace> namespaceMap = new TreeMap<>();
	private final Map<String,AuxiliaryDatabaseObject> auxiliaryDatabaseObjects = new LinkedHashMap<>();
	private transient ServiceRegistry serviceRegistry;
	private transient PhysicalNamingStrategy physicalNamingStrategy;

	private transient PhysicalNamespaceName physicalImplicitNamespaceName;
	private PhysicalNamespaceSnapshot physicalImplicitNamespaceSnapshot;
	private List<InitCommand> initCommands;

	public Database(MappingResolutionOptions buildingPlan) {
		this( buildingPlan, buildingPlan.getServiceRegistry().getService( JdbcEnvironment.class ) );
	}

	public Database(MappingResolutionOptions buildingOptions, JdbcEnvironment jdbcEnvironment) {
		serviceRegistry = buildingOptions.getServiceRegistry();
		this.jdbcEnvironment = scopedJdbcEnvironment( buildingOptions, jdbcEnvironment );
		typeConfiguration = buildingOptions.getTypeConfiguration();
		physicalNamingStrategy = buildingOptions.getPhysicalNamingStrategy();
		dialect = determineDialect( buildingOptions );

		setImplicitNamespaceName(
				toLogicalName( buildingOptions.getMappingDefaults().getImplicitCatalogName(), false ),
				toLogicalName( buildingOptions.getMappingDefaults().getImplicitSchemaName(), false )
		);
	}

	private JdbcEnvironment scopedJdbcEnvironment(MappingResolutionOptions options, JdbcEnvironment environment) {
		return environment == null ? null : new PersistenceUnitJdbcEnvironment(
				environment,
				options.getMappingDefaults()::shouldImplicitlyQuoteIdentifiers,
				globallyQuoteIdentifiersSkipColumnDefinitions( serviceRegistry )
		);
	}

	private static boolean globallyQuoteIdentifiersSkipColumnDefinitions(ServiceRegistry serviceRegistry) {
		try {
			final var configurationService = serviceRegistry.getService( ConfigurationService.class );
			return configurationService != null
				&& configurationService.getSetting(
						GLOBALLY_QUOTED_IDENTIFIERS_SKIP_COLUMN_DEFINITIONS,
						BOOLEAN,
						false
				);
		}
		catch (UnknownServiceException ignored) {
			return false;
		}
	}

	private void setImplicitNamespaceName(LogicalName catalogName, LogicalName schemaName) {
		physicalImplicitNamespaceName = new PhysicalNamespaceName(
				PhysicalNamingStrategyHelper.resolve( catalogName, jdbcEnvironment, physicalNamingStrategy::toPhysicalCatalogName, "catalog", true ),
				PhysicalNamingStrategyHelper.resolve( schemaName, jdbcEnvironment, physicalNamingStrategy::toPhysicalSchemaName, "schema", true )
		);
		physicalImplicitNamespaceSnapshot = PhysicalNamespaceSnapshot.from( physicalImplicitNamespaceName );
	}

	/// Supply already-physical default qualifiers, as used by JDBC reverse engineering.
	public void setPhysicalImplicitNamespaceName(PhysicalNamespaceName name) {
		physicalImplicitNamespaceName = java.util.Objects.requireNonNull( name );
		physicalImplicitNamespaceSnapshot = PhysicalNamespaceSnapshot.from( name );
	}

	private static Dialect determineDialect(MappingResolutionOptions buildingPlan) {
		final Dialect dialect = buildingPlan.getServiceRegistry().requireService( JdbcServices.class ).getDialect();
		if ( dialect != null ) {
			return dialect;
		}

		// Use H2 dialect as default
		return new H2Dialect();
	}

	private Namespace makeNamespace(Namespace.LogicalNamespaceName name) {
		final Namespace namespace = new Namespace( getPhysicalNamingStrategy(), getJdbcEnvironment(), name );
		namespaceMap.put( name, namespace );
		return namespace;
	}

	public Dialect getDialect() {
		return dialect;
	}

	public JdbcEnvironment getJdbcEnvironment() {
		return jdbcEnvironment;
	}

	/**
	 * Wrap raw name text in its Identifier form, preserving explicit source quoting.
	 * Global and automatic quoting are applied after physical naming.
	 *
	 * @implNote Global and database keyword quoting happen only when finalizing physical identifiers.
	 *
	 * @param text The raw object name
	 *
	 * @return The wrapped Identifier form
	 */
	public Identifier toIdentifier(String text) {
		return toIdentifier( text, false );
	}

	/**
	 * Wrap raw name text in its Identifier form, preserving explicit source quoting.
	 * Global and automatic quoting are applied after physical naming.
	 *
	 * @param text The raw object name
	 * @param isExplicit Whether the name is explicitly set
	 * @return The wrapped Identifier form
	 * @implNote Global and database keyword quoting happen only when finalizing physical identifiers.
	 */
	public Identifier toIdentifier(String text, boolean isExplicit) {
		return text == null ? null : Identifier.toIdentifier( text, false, false, isExplicit );
	}

	/**
	 * Interpret an explicitly supplied logical name, preserving source quoting.
	 * No physical transformation or automatic quoting is applied.
	 */
	public LogicalName toLogicalName(String text) {
		return toLogicalName( text, true );
	}

	/**
	 * Interpret logical source text with its explicit or generated origin.
	 */
	public LogicalName toLogicalName(String text, boolean explicit) {
		final var identifier = Identifier.toIdentifier( text, false, false, explicit );
		return identifier == null ? null : new LogicalName( identifier.getText(), identifier.isQuoted(), explicit );
	}

	public PhysicalNamingStrategy getPhysicalNamingStrategy() {
		return physicalNamingStrategy;
	}

	public Iterable<Namespace> getNamespaces() {
		return namespaceMap.values();
	}

	/**
	 * @return The default namespace, with a {@code null} catalog and schema
	 *         which will have to be interpreted with defaults at runtime.
	 * @see SqlStringGenerationContext
	 */
	public Namespace getDefaultNamespace() {
		return locateNamespace( null, null );
	}

	/**
	 * @return The implicit name of the default namespace, with a {@code null}
	 *         catalog and schema which will have to be interpreted with defaults
	 *         at runtime.
	 * @see SqlStringGenerationContext
	 */
	public PhysicalNamespaceName getPhysicalImplicitNamespaceName() {
		if ( physicalImplicitNamespaceName == null ) {
			throw new IllegalStateException( "Database services must be reattached before accessing physical names" );
		}
		return physicalImplicitNamespaceName;
	}

	public @Nullable Namespace findNamespace(LogicalName catalogName, LogicalName schemaName) {
		return namespaceMap.get( new Namespace.LogicalNamespaceName( catalogName, schemaName ) );
	}

	public Namespace locateNamespace(LogicalName catalogName, LogicalName schemaName) {
		final var name = new Namespace.LogicalNamespaceName( catalogName, schemaName );
		final var namespace = namespaceMap.get( name );
		return namespace == null ? makeNamespace( name ) : namespace;
	}

	/// Locate a namespace for names already obtained from the database, without naming or quoting them again.
	public Namespace locatePhysicalNamespace(
			PhysicalName catalog,
			PhysicalName schema) {
		final var name = new Namespace.LogicalNamespaceName( physicalLookupName( catalog ), physicalLookupName( schema ) );
		final var physicalName = new PhysicalNamespaceName( catalog, schema );
		final var namespace = namespaceMap.get( name );
		if ( namespace != null ) {
			if ( !namespace.getPhysicalName().equals( physicalName ) ) {
				throw new MappingException( "Namespace lookup already denotes different physical names: " + name );
			}
			return namespace;
		}
		final var created = new Namespace( physicalNamingStrategy, jdbcEnvironment, name, physicalName );
		namespaceMap.put( name, created );
		return created;
	}

	private static LogicalName physicalLookupName(PhysicalName name) {
		return name == null ? null : new LogicalName( name.getText(), name.isQuoted(), true );
	}

	public Namespace adjustDefaultNamespace(LogicalName catalogName, LogicalName schemaName) {
		setImplicitNamespaceName( catalogName, schemaName );
		return locateNamespace( catalogName, schemaName );
	}

	public Namespace adjustDefaultNamespace(String implicitCatalogName, String implicitSchemaName) {
		return adjustDefaultNamespace( toLogicalName( implicitCatalogName, false ), toLogicalName( implicitSchemaName, false ) );
	}

	public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {
		auxiliaryDatabaseObjects.put( auxiliaryDatabaseObject.getExportIdentifier(), auxiliaryDatabaseObject );
	}

	public Collection<AuxiliaryDatabaseObject> getAuxiliaryDatabaseObjects() {
		return auxiliaryDatabaseObjects.values();
	}

	public Collection<InitCommand> getInitCommands() {
		return initCommands == null ? emptyList() : initCommands;
	}

	public void addInitCommand(InitCommand initCommand) {
		if ( initCommands == null ) {
			initCommands = new ArrayList<>();
		}
		initCommands.add( initCommand );
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	public void reattach(MappingResolutionOptions buildingPlan) {
		serviceRegistry = buildingPlan.getServiceRegistry();
		typeConfiguration = buildingPlan.getTypeConfiguration();
		jdbcEnvironment = scopedJdbcEnvironment( buildingPlan, serviceRegistry.getService( JdbcEnvironment.class ) );
		physicalNamingStrategy = buildingPlan.getPhysicalNamingStrategy();
		dialect = determineDialect( buildingPlan );
		physicalImplicitNamespaceName = physicalImplicitNamespaceSnapshot.restore( jdbcEnvironment.getIdentifierHelper().getPhysicalNameFactory() );
		namespaceMap.values().forEach( namespace -> namespace.reattach( physicalNamingStrategy, jdbcEnvironment ) );
		final var columnNames = new org.hibernate.mapping.ColumnNameLifecycle();
		namespaceMap.values().forEach( namespace -> {
			namespace.getTables().forEach( columnNames::addContainer );
			namespace.getUserDefinedTypes().forEach( type -> {
				if ( type instanceof org.hibernate.mapping.UserDefinedObjectType objectType ) {
					columnNames.addUserDefinedType( objectType );
				}
			} );
		} );
		columnNames.restore( jdbcEnvironment.getIdentifierHelper().getPhysicalNameFactory() );
	}
}
