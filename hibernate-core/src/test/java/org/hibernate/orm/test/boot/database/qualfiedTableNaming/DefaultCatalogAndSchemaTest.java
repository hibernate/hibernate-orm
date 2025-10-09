/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.database.qualfiedTableNaming;

import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.internal.SessionFactoryBuilderImpl;
import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.PersistentTableStrategy;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationOperationGroup;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModelFunctionalTesting;
import org.hibernate.testing.orm.junit.DomainModelProducer;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.SessionFactoryFunctionalTesting;
import org.hibernate.testing.orm.junit.SessionFactoryProducer;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SessionFactoryScopeAware;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToWriter;
import org.hibernate.tool.schema.spi.DelayedDropRegistryNotAvailableImpl;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.TargetDescriptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A few important JUnit points - <ol>
 *     <li>
 *         We are using {@linkplain ParameterizedClass} to be able to
 *         ctor-inject the various {@linkplain #options() catalog/schema parameter}
 *         combinations.  JUnit requires {@linkplain TestInstance.Lifecycle#PER_METHOD}
 *         for this.
 *     </li>
 *     <li>
 *         We cannot directly use {@linkplain org.hibernate.testing.orm.junit.ServiceRegistry},
 *         {@linkplain org.hibernate.testing.orm.junit.DomainModel} or
 *         {@linkplain org.hibernate.testing.orm.junit.SessionFactory} because of the
 *         need to dynamically create these based on the {@linkplain #options() catalog/schema parameters}.
 *         So instead we use {@linkplain ServiceRegistryProducer,
 *         {@linkplain DomainModelProducer} and {@linkplain SessionFactoryProducer} while
 *         still using the extensions to leverage lifecycle.
 *     </li>
 *     <li>
 *         ParameterizedClass cannot be used to ctor-inject things resolved via a
 *         {@linkplain org.junit.jupiter.api.extension.ParameterResolver}.  So we leverage
 *         {@linkplain SessionFactoryScopeAware} to inject the {@linkplain SessionFactoryScope}
 *         as an inst var to avoid passing it around to all delegate methods.
 *     </li>
 * </ol>
 */
@JiraKey( value = "HHH-14921" )
@JiraKey( value = "HHH-14922" )
@JiraKey( value = "HHH-15212" )
@JiraKey( value = "HHH-16177" )
@RequiresDialectFeature(feature= DialectFeatureChecks.SupportsIdentityColumns.class)
@TestInstance( TestInstance.Lifecycle.PER_METHOD )
@ParameterizedClass
@MethodSource("options")
@ServiceRegistryFunctionalTesting
@DomainModelFunctionalTesting
@SessionFactoryFunctionalTesting
public class DefaultCatalogAndSchemaTest
		implements ServiceRegistryProducer, DomainModelProducer, SessionFactoryProducer, SessionFactoryScopeAware {

	private static final String SQL_QUOTE_CHARACTER_CLASS = "([`\"]|\\[|\\])";

	private static final String EXPLICIT_CATALOG = "someExplicitCatalog";
	private static final String EXPLICIT_SCHEMA = "someExplicitSchema";
	private static final String IMPLICIT_FILE_LEVEL_CATALOG = "someImplicitFileLevelCatalog";
	private static final String IMPLICIT_FILE_LEVEL_SCHEMA = "someImplicitFileLevelSchema";

	// Yes this is invalid SQL, and in most cases it simply wouldn't work because of missing columns,
	// but in this case we don't care: we just want to check catalog/schema substitution.
	private static final String CUSTOM_INSERT_SQL_PART_1 = "insert into {h-catalog}{h-schema}";
	private static final String CUSTOM_INSERT_SQL_PART_2 = ", {h-domain}";
	private static final String CUSTOM_INSERT_SQL_PART_3 = " VALUES(basic = ?)";
	private static final String CUSTOM_UPDATE_SQL_PART_1 = "update {h-catalog}{h-schema}";
	private static final String CUSTOM_UPDATE_SQL_PART_2 = ", {h-domain}";
	private static final String CUSTOM_UPDATE_SQL_PART_3 = " SET basic = ?";
	private static final String CUSTOM_DELETE_SQL_PART_1 = "delete from {h-catalog}{h-schema}";
	private static final String CUSTOM_DELETE_SQL_PART_2 = ", {h-domain}";
	private static final String CUSTOM_DELETE_SQL_PART_3 = " WHERE id = ?";


	enum SettingsMode {
		// "Standard" way of providing settings, though configuration properties:
		// both metadata and session factory receive the same settings
		METADATA_SERVICE_REGISTRY,
		// An alternative way of providing settings so that they are applied late,
		// when the session factory is created.
		// This mode is used by frameworks relying on build-time initialization of the application,
		// like Quarkus and its "static init".
		SESSION_FACTORY_SERVICE_REGISTRY
	}

	public record Options(
			SettingsMode settingsMode,
			String xmlMapping,
			String defaultCatalog,
			String defaultSchema,
			String expectedDefaultCatalog,
			String expectedDefaultSchema) {
	}

	public static List<Options> options() {
		final List<Options> options = new ArrayList<>();

		for ( SettingsMode mode : SettingsMode.values() ) {
			for ( String defaultCatalog : Arrays.asList( null, "someDefaultCatalog" ) ) {
				for ( String defaultSchema : Arrays.asList( null, "someDefaultSchema" ) ) {
					options.add( new Options(
							mode,
							null,
							defaultCatalog,
							defaultSchema,
							// The default catalog/schema should be used when
							// there is no implicit catalog/schema defined in the mapping.
							defaultCatalog,
							defaultSchema
					) );
				}
			}

			options.add( new Options(
					mode,
					"implicit-global-catalog-and-schema.orm.xml",
					null,
					null,
					"someImplicitCatalog",
					"someImplicitSchema"
			) );

			// HHH-14922: Inconsistent precedence of orm.xml implicit catalog/schema over "default_catalog"/"default_schema"
			options.add( new Options(
					mode,
					"implicit-global-catalog-and-schema.orm.xml",
					"someDefaultCatalog",
					"someDefaultSchema",
					"someDefaultCatalog",
					"someDefaultSchema"
			) );
		}

		return options;
	}

	private final Options options;

	private boolean dbSupportsCatalogs;
	private boolean dbSupportsSchemas;

	private SessionFactoryScope factoryScope;

	private final List<AutoCloseable> autoCloseables = new ArrayList<>();

	public DefaultCatalogAndSchemaTest(Options options) {
		this.options = options;
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		switch ( options.settingsMode ) {
			case METADATA_SERVICE_REGISTRY:
				configureServiceRegistry( options.defaultCatalog, options.defaultSchema, builder );
				break;
			case SESSION_FACTORY_SERVICE_REGISTRY:
				configureServiceRegistry( null, null, builder );
				break;
			default:
				throw new IllegalStateException( "Unknown settings mode: " + options.settingsMode );
		}

		return builder.build();
	}

	private void configureServiceRegistry(String defaultCatalog, String defaultSchema, StandardServiceRegistryBuilder builder) {
		builder.applySetting( PersistentTableStrategy.DROP_ID_TABLES, "true" );
		builder.applySetting( GlobalTemporaryTableStrategy.DROP_ID_TABLES, "true" );
		builder.applySetting( LocalTemporaryTableStrategy.DROP_ID_TABLES, "true" );
		builder.applySetting( AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS, "true" );
		if ( defaultCatalog != null ) {
			builder.applySetting( AvailableSettings.DEFAULT_CATALOG, defaultCatalog );
		}
		if ( defaultSchema != null ) {
			builder.applySetting( AvailableSettings.DEFAULT_SCHEMA, defaultSchema );
		}
	}

	@Override
	public MetadataImplementor produceModel(StandardServiceRegistry serviceRegistry) {
		final MetadataSources metadataSources = new MetadataSources( serviceRegistry );
		metadataSources.addInputStream( getClass().getResourceAsStream( "implicit-file-level-catalog-and-schema.orm.xml" ) );
		metadataSources.addInputStream( getClass().getResourceAsStream( "implicit-file-level-catalog-and-schema.hbm.xml" ) );
		metadataSources.addInputStream( getClass().getResourceAsStream( "no-file-level-catalog-and-schema.orm.xml" ) );
		metadataSources.addInputStream( getClass().getResourceAsStream( "no-file-level-catalog-and-schema.hbm.xml" ) );
		metadataSources.addInputStream( getClass().getResourceAsStream( "database-object-using-catalog-placeholder.orm.xml" ) );
		metadataSources.addInputStream( getClass().getResourceAsStream( "database-object-using-schema-placeholder.orm.xml" ) );
		if ( options.xmlMapping != null ) {
			metadataSources.addInputStream( getClass().getResourceAsStream( options.xmlMapping ) );
		}
		metadataSources.addAnnotatedClasses(
				EntityWithDefaultQualifiers.class,
				EntityWithExplicitQualifiers.class,
				EntityWithJoinedInheritanceWithDefaultQualifiers.class,
				EntityWithJoinedInheritanceWithDefaultQualifiersSubclass.class,
				EntityWithJoinedInheritanceWithExplicitQualifiers.class,
				EntityWithJoinedInheritanceWithExplicitQualifiersSubclass.class,
				EntityWithTablePerClassInheritanceWithDefaultQualifiers.class,
				EntityWithTablePerClassInheritanceWithDefaultQualifiersSubclass.class,
				EntityWithTablePerClassInheritanceWithExplicitQualifiers.class,
				EntityWithTablePerClassInheritanceWithExplicitQualifiersSubclass.class,
				EntityWithDefaultQualifiersWithCustomSql.class,
				EntityWithDefaultQualifiersWithIdentityGenerator.class,
				EntityWithExplicitQualifiersWithIdentityGenerator.class,
				EntityWithDefaultQualifiersWithTableGenerator.class,
				EntityWithExplicitQualifiersWithTableGenerator.class,
				EntityWithDefaultQualifiersWithSequenceGenerator.class,
				EntityWithExplicitQualifiersWithSequenceGenerator.class,
				EntityWithDefaultQualifiersWithIncrementGenerator.class,
				EntityWithExplicitQualifiersWithIncrementGenerator.class,
				EntityWithDefaultQualifiersWithEnhancedSequenceGenerator.class,
				EntityWithExplicitQualifiersWithEnhancedSequenceGenerator.class
		);

		return (MetadataImplementor) metadataSources.buildMetadata();
	}

	@Override
	public SessionFactoryImplementor produceSessionFactory(MetadataImplementor model) {
		SessionFactoryBuilder sfb;
		switch ( options.settingsMode ) {
			case METADATA_SERVICE_REGISTRY:
				sfb = model.getSessionFactoryBuilder();
				break;
			case SESSION_FACTORY_SERVICE_REGISTRY:
				var srb = ServiceRegistryUtil.serviceRegistryBuilder();
				configureServiceRegistry( options.defaultCatalog, options.defaultSchema, srb );
				final StandardServiceRegistry sr = srb.build();
				autoCloseables.add( sr );
				var bootstrapContext = ((MetadataImpl) model).getBootstrapContext();
				sfb = new SessionFactoryBuilderImpl(
						model,
						new SessionFactoryOptionsBuilder( sr, bootstrapContext ),
						bootstrapContext
				);
				break;
			default:
				throw new IllegalStateException( "Unknown settings mode: " + options.settingsMode );
		}

		return (SessionFactoryImplementor) sfb.build();
	}

	@Override
	public void injectSessionFactoryScope(SessionFactoryScope factoryScope) {
		this.factoryScope = factoryScope;

		var sessionFactory = factoryScope.getSessionFactory();
		var nameQualifierSupport = sessionFactory.getJdbcServices().getJdbcEnvironment().getNameQualifierSupport();
		dbSupportsCatalogs = nameQualifierSupport.supportsCatalogs();
		dbSupportsSchemas = nameQualifierSupport.supportsSchemas();
	}

	@AfterEach
	public void handleAutoClosing() throws Throwable {
		Throwable thrown = null;
		for ( AutoCloseable closeable : autoCloseables ) {
			try {
				closeable.close();
			}
			catch (Throwable t) {
				if ( thrown == null ) {
					thrown = t;
				}
				else {
					thrown.addSuppressed( t );
				}
			}
		}
		if ( thrown != null ) {
			throw thrown;
		}

		autoCloseables.clear();
	}

	@Test
	public void createSchema_fromSessionFactory(DomainModelScope modelScope) {
		String script = generateScriptFromSessionFactory( "create", modelScope, factoryScope );
		verifyDDLCreateCatalogOrSchema( script );
		verifyDDLQualifiers( script );
	}

	@Test
	@SkipForDialect(dialectClass = SQLServerDialect.class,
			reason = "SQL Server support catalogs but their implementation of DatabaseMetaData"
					+ " throws exceptions when calling getSchemas/getTables with a non-existing catalog,"
					+ " which results in nasty errors when generating an update script"
					+ " and some catalogs don't exist.")
	@SkipForDialect(dialectClass = SybaseDialect.class,
			matchSubTypes = true,
			reason = "Sybase support catalogs but their implementation of DatabaseMetaData"
					+ " throws exceptions when calling getSchemas/getTables with a non-existing catalog,"
					+ " which results in nasty errors when generating an update script"
					+ " and some catalogs don't exist.")
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Informix support catalogs but their implementation of DatabaseMetaData"
					+ " throws exceptions when calling getSchemas/getTables with a non-existing catalog,"
					+ " which results in nasty errors when generating an update script"
					+ " and some catalogs don't exist.")
	public void updateSchema_fromSessionFactory(DomainModelScope modelScope) {
		String script = generateScriptFromSessionFactory( "update", modelScope, factoryScope );
		verifyDDLCreateCatalogOrSchema( script );
		verifyDDLQualifiers( script );
	}

	@Test
	public void dropSchema_fromSessionFactory(DomainModelScope modelScope) {
		String script = generateScriptFromSessionFactory( "drop",  modelScope, factoryScope );
		verifyDDLDropCatalogOrSchema( script );
		verifyDDLQualifiers( script );
	}

	@Test
	public void createSchema_fromMetadata(DomainModelScope modelScope) {
		String script = generateScriptFromMetadata( SchemaExport.Action.CREATE, modelScope );
		verifyDDLCreateCatalogOrSchema( script );
		verifyDDLQualifiers( script );
	}

	@Test
	public void dropSchema_fromMetadata(DomainModelScope modelScope) {
		String script = generateScriptFromMetadata( SchemaExport.Action.DROP, modelScope );
		verifyDDLDropCatalogOrSchema( script );
		verifyDDLQualifiers( script );
	}

	@Test
	public void entityPersister() {
		verifyEntityPersisterQualifiers( EntityWithDefaultQualifiers.class, expectedDefaultQualifier() );
		verifyEntityPersisterQualifiers( EntityWithExplicitQualifiers.class, expectedExplicitQualifier() );
		verifyEntityPersisterQualifiers( EntityWithOrmXmlImplicitFileLevelQualifiers.class, expectedImplicitFileLevelQualifier() );
		verifyEntityPersisterQualifiers( EntityWithHbmXmlImplicitFileLevelQualifiers.class, expectedImplicitFileLevelQualifier() );
		verifyEntityPersisterQualifiers( EntityWithOrmXmlNoFileLevelQualifiers.class, expectedDefaultQualifier() );
		verifyEntityPersisterQualifiers( EntityWithHbmXmlNoFileLevelQualifiers.class, expectedDefaultQualifier() );

		verifyEntityPersisterQualifiers( EntityWithJoinedInheritanceWithDefaultQualifiers.class, expectedDefaultQualifier() );
		verifyEntityPersisterQualifiers( EntityWithJoinedInheritanceWithDefaultQualifiersSubclass.class, expectedDefaultQualifier() );
		verifyEntityPersisterQualifiers( EntityWithJoinedInheritanceWithExplicitQualifiers.class, expectedExplicitQualifier() );
		verifyEntityPersisterQualifiers( EntityWithJoinedInheritanceWithExplicitQualifiersSubclass.class, expectedExplicitQualifier() );

		verifyEntityPersisterQualifiers( EntityWithTablePerClassInheritanceWithDefaultQualifiers.class, expectedDefaultQualifier() );
		verifyEntityPersisterQualifiers( EntityWithTablePerClassInheritanceWithDefaultQualifiersSubclass.class, expectedDefaultQualifier() );
		verifyEntityPersisterQualifiers( EntityWithTablePerClassInheritanceWithExplicitQualifiers.class, expectedExplicitQualifier() );
		verifyEntityPersisterQualifiers( EntityWithTablePerClassInheritanceWithExplicitQualifiersSubclass.class, expectedExplicitQualifier() );

		verifyEntityPersisterQualifiers( EntityWithDefaultQualifiersWithCustomSql.class, expectedDefaultQualifier() );


		verifyEntityPersisterQualifiers( EntityWithDefaultQualifiersWithIdentityGenerator.class, expectedDefaultQualifier() );
		verifyEntityPersisterQualifiers( EntityWithExplicitQualifiersWithIdentityGenerator.class, expectedExplicitQualifier() );
		verifyEntityPersisterQualifiers( EntityWithDefaultQualifiersWithTableGenerator.class, expectedDefaultQualifier() );
		verifyEntityPersisterQualifiers( EntityWithExplicitQualifiersWithTableGenerator.class, expectedExplicitQualifier() );
		verifyEntityPersisterQualifiers( EntityWithDefaultQualifiersWithSequenceGenerator.class, expectedDefaultQualifier() );
		verifyEntityPersisterQualifiers( EntityWithExplicitQualifiersWithSequenceGenerator.class, expectedExplicitQualifier() );
		verifyEntityPersisterQualifiers( EntityWithDefaultQualifiersWithIncrementGenerator.class, expectedDefaultQualifier() );
		verifyEntityPersisterQualifiers( EntityWithExplicitQualifiersWithIncrementGenerator.class, expectedExplicitQualifier() );
		verifyEntityPersisterQualifiers( EntityWithDefaultQualifiersWithEnhancedSequenceGenerator.class, expectedDefaultQualifier() );
		verifyEntityPersisterQualifiers( EntityWithExplicitQualifiersWithEnhancedSequenceGenerator.class, expectedExplicitQualifier() );
	}

	private void verifyEntityPersisterQualifiers(
			Class<?> entityClass,
			ExpectedQualifier expectedQualifier) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		// The hbm.xml mapping unfortunately sets the native entity name on top of the JPA entity name,
		// so many methods that allow retrieving the entity persister or entity metamodel from the entity class no longer work,
		// because these methods generally assume the native entity name is the FQCN.
		// Thus we use custom code.
		AbstractEntityPersister persister = (AbstractEntityPersister) sessionFactory.getRuntimeMetamodels().getMappingMetamodel().streamEntityDescriptors()
				.filter( p -> p.getMappedClass().equals( entityClass ) )
				.findFirst()
				.orElseThrow( () -> new IllegalStateException( "Cannot find persister for " + entityClass ) );
		String jpaEntityName = sessionFactory.getJpaMetamodel().getEntities()
				.stream()
				.filter( p -> p.getJavaType().equals( entityClass ) )
				.findFirst()
				.orElseThrow( () -> new IllegalStateException( "Cannot find entity metamodel for " + entityClass ) )
				.getName();

		// Table names are what's used for Query, in particular.
		verifyOnlyQualifier( persister.getTableName(), SqlType.RUNTIME,
				jpaEntityName, expectedQualifier );
		// Here, to simplify assertions, we assume all derived entity types have:
		// - an entity name prefixed with the name of their super entity type
		// - the same explicit catalog and schema, if any, as their super entity type
		verifyOnlyQualifier( persister.getTableNames(), SqlType.RUNTIME,
				jpaEntityName, expectedQualifier );

		// This will include SQL generated by ID generators in some cases, which will be validated here
		// because ID generators table/sequence names are prefixed with the owning entity name.

		{
			final MutationOperationGroup staticSqlInsertGroup = persister.getInsertCoordinator().getStaticMutationOperationGroup();
			final String[] insertSqls = new String[staticSqlInsertGroup.getNumberOfOperations()];
			for ( int tablePosition = 0;
					tablePosition < staticSqlInsertGroup.getNumberOfOperations();
					tablePosition++ ) {
				final MutationOperation insertOperation = staticSqlInsertGroup.getOperation( tablePosition );
				if ( insertOperation instanceof PreparableMutationOperation ) {
					insertSqls[tablePosition] = ( (PreparableMutationOperation) insertOperation ).getSqlString();
				}
			}
			verifyOnlyQualifier( insertSqls, SqlType.RUNTIME, jpaEntityName, expectedQualifier );
		}

		String identitySelectString = persister.getIdentitySelectString();
		if ( identitySelectString != null ) {
			verifyOnlyQualifierOptional( identitySelectString, SqlType.RUNTIME, jpaEntityName, expectedQualifier );
		}

		{
			final MutationOperationGroup staticSqlUpdateGroup = persister.getUpdateCoordinator().getStaticMutationOperationGroup();
			final String[] sqlUpdateStrings = new String[staticSqlUpdateGroup.getNumberOfOperations()];
			for ( int tablePosition = 0;
					tablePosition < staticSqlUpdateGroup.getNumberOfOperations();
					tablePosition++ ) {
				final MutationOperation operation = staticSqlUpdateGroup.getOperation( tablePosition );
				if ( operation instanceof PreparableMutationOperation ) {
					sqlUpdateStrings[tablePosition] = ( (PreparableMutationOperation) operation ).getSqlString();
				}
			}
			verifyOnlyQualifier( sqlUpdateStrings, SqlType.RUNTIME, jpaEntityName, expectedQualifier );
		}


		{
			final MutationOperationGroup staticDeleteGroup = persister.getDeleteCoordinator().getStaticMutationOperationGroup();
			final String[] sqlDeleteStrings = new String[staticDeleteGroup.getNumberOfOperations()];
			for ( int tablePosition = 0; tablePosition < staticDeleteGroup.getNumberOfOperations(); tablePosition++ ) {
				final MutationOperation operation = staticDeleteGroup.getOperation( tablePosition );
				if ( operation instanceof PreparableMutationOperation ) {
					sqlDeleteStrings[tablePosition] = ( (PreparableMutationOperation) operation ).getSqlString();
				}
			}
			verifyOnlyQualifier( sqlDeleteStrings, SqlType.RUNTIME, jpaEntityName, expectedQualifier );
		}

		// This is used in the "select" id generator in particular.
		verifyOnlyQualifierOptional( persister.getSelectByUniqueKeyString( "basic" ), SqlType.RUNTIME,
				jpaEntityName, expectedQualifier );
	}

	@Test
	public void tableGenerator() {
		org.hibernate.id.enhanced.TableGenerator generator = idGenerator(
				org.hibernate.id.enhanced.TableGenerator.class,
						EntityWithDefaultQualifiersWithTableGenerator.class );
		verifyOnlyQualifier( generator.getAllSqlForTests(), SqlType.RUNTIME,
				EntityWithDefaultQualifiersWithTableGenerator.NAME, expectedDefaultQualifier() );

		generator = idGenerator( org.hibernate.id.enhanced.TableGenerator.class,
				EntityWithExplicitQualifiersWithTableGenerator.class );
		verifyOnlyQualifier( generator.getAllSqlForTests(), SqlType.RUNTIME,
				EntityWithExplicitQualifiersWithTableGenerator.NAME, expectedExplicitQualifier() );
	}

	@Test
	public void enhancedTableGenerator() {
		org.hibernate.id.enhanced.TableGenerator generator = idGenerator(
				org.hibernate.id.enhanced.TableGenerator.class,
				EntityWithDefaultQualifiersWithTableGenerator.class );
		verifyOnlyQualifier( generator.getAllSqlForTests(), SqlType.RUNTIME,
				EntityWithDefaultQualifiersWithTableGenerator.NAME, expectedDefaultQualifier() );

		generator = idGenerator( org.hibernate.id.enhanced.TableGenerator.class,
				EntityWithExplicitQualifiersWithTableGenerator.class );
		verifyOnlyQualifier( generator.getAllSqlForTests(), SqlType.RUNTIME,
				EntityWithExplicitQualifiersWithTableGenerator.NAME, expectedExplicitQualifier() );
	}

	@Test
	public void sequenceGenerator() {
		org.hibernate.id.enhanced.SequenceStyleGenerator generator = idGenerator(
				org.hibernate.id.enhanced.SequenceStyleGenerator.class,
				EntityWithDefaultQualifiersWithSequenceGenerator.class );
		verifyOnlyQualifier( generator.getDatabaseStructure().getAllSqlForTests(), SqlType.RUNTIME,
				EntityWithDefaultQualifiersWithSequenceGenerator.NAME, expectedDefaultQualifier() );

		generator = idGenerator( org.hibernate.id.enhanced.SequenceStyleGenerator.class,
				EntityWithExplicitQualifiersWithSequenceGenerator.class );
		verifyOnlyQualifier( generator.getDatabaseStructure().getAllSqlForTests(), SqlType.RUNTIME,
				EntityWithExplicitQualifiersWithSequenceGenerator.NAME, expectedExplicitQualifier() );
	}

	@Test
	public void enhancedSequenceGenerator() {
		org.hibernate.id.enhanced.SequenceStyleGenerator generator = idGenerator(
				org.hibernate.id.enhanced.SequenceStyleGenerator.class,
				EntityWithDefaultQualifiersWithEnhancedSequenceGenerator.class );
		verifyOnlyQualifier( generator.getDatabaseStructure().getAllSqlForTests(), SqlType.RUNTIME,
				EntityWithDefaultQualifiersWithEnhancedSequenceGenerator.NAME, expectedDefaultQualifier() );

		generator = idGenerator( org.hibernate.id.enhanced.SequenceStyleGenerator.class,
				EntityWithExplicitQualifiersWithEnhancedSequenceGenerator.class );
		verifyOnlyQualifier( generator.getDatabaseStructure().getAllSqlForTests(), SqlType.RUNTIME,
				EntityWithExplicitQualifiersWithEnhancedSequenceGenerator.NAME, expectedExplicitQualifier() );
	}

	@Test
	public void incrementGenerator() {
		org.hibernate.id.IncrementGenerator generator = idGenerator( org.hibernate.id.IncrementGenerator.class,
				EntityWithDefaultQualifiersWithIncrementGenerator.class );
		verifyOnlyQualifier( generator.getAllSqlForTests(), SqlType.RUNTIME,
				EntityWithDefaultQualifiersWithIncrementGenerator.NAME, expectedDefaultQualifier() );

		generator = idGenerator( org.hibernate.id.IncrementGenerator.class,
				EntityWithExplicitQualifiersWithIncrementGenerator.class );
		verifyOnlyQualifier( generator.getAllSqlForTests(), SqlType.RUNTIME,
				EntityWithExplicitQualifiersWithIncrementGenerator.NAME, expectedExplicitQualifier() );
	}

	private <T extends IdentifierGenerator> T idGenerator(Class<T> expectedType, Class<?> entityClass) {
		final AbstractEntityPersister persister = (AbstractEntityPersister) factoryScope.getSessionFactory().getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( entityClass );
		return expectedType.cast( persister.getIdentifierGenerator() );
	}

	private void verifyDDLCreateCatalogOrSchema(String sql) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		final Dialect dialect = sessionFactory.getJdbcServices().getDialect();

		if ( sessionFactory.getJdbcServices().getDialect().canCreateCatalog() ) {
			assertThat( sql ).contains( dialect.getCreateCatalogCommand( EXPLICIT_CATALOG ) );
			assertThat( sql ).contains( dialect.getCreateCatalogCommand( IMPLICIT_FILE_LEVEL_CATALOG ) );
			if ( options.expectedDefaultCatalog != null ) {
				assertThat( sql ).contains( dialect.getCreateCatalogCommand( options.expectedDefaultCatalog ) );
			}
		}

		if ( sessionFactory.getJdbcServices().getDialect().canCreateSchema() ) {
			assertThat( sql ).contains( dialect.getCreateSchemaCommand( EXPLICIT_SCHEMA ) );
			assertThat( sql ).contains( dialect.getCreateSchemaCommand( IMPLICIT_FILE_LEVEL_SCHEMA ) );
			if ( options.expectedDefaultSchema != null ) {
				assertThat( sql ).contains( dialect.getCreateSchemaCommand( options.expectedDefaultSchema ) );
			}
		}
	}

	private void verifyDDLDropCatalogOrSchema(String sql) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();
		final Dialect dialect = sessionFactory.getJdbcServices().getDialect();

		if ( sessionFactory.getJdbcServices().getDialect().canCreateCatalog() ) {
			assertThat( sql ).contains( dialect.getDropCatalogCommand( EXPLICIT_CATALOG ) );
			assertThat( sql ).contains( dialect.getDropCatalogCommand( IMPLICIT_FILE_LEVEL_CATALOG ) );
			if ( options.expectedDefaultCatalog != null ) {
				assertThat( sql ).contains( dialect.getDropCatalogCommand( options.expectedDefaultCatalog ) );
			}
		}

		if ( sessionFactory.getJdbcServices().getDialect().canCreateSchema() ) {
			assertThat( sql ).contains( dialect.getDropSchemaCommand( EXPLICIT_SCHEMA ) );
			assertThat( sql ).contains( dialect.getDropSchemaCommand( IMPLICIT_FILE_LEVEL_SCHEMA ) );
			if ( options.expectedDefaultSchema != null ) {
				assertThat( sql ).contains( dialect.getDropSchemaCommand( options.expectedDefaultSchema ) );
			}
		}
	}

	private void verifyDDLQualifiers(String sql) {
		// Here, to simplify assertions, we assume:
		// - that all entity types have a table name identical to the entity name
		// - that all association tables have a name prefixed with the name of their owning entity type
		// - that all association tables have the same explicit catalog and schema, if any, as their owning entity type
		// - that all ID generator tables/sequences have a name prefixed with the name of their owning entity type

		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithExplicitQualifiers.NAME, expectedExplicitQualifier() );
		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithDefaultQualifiers.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithOrmXmlImplicitFileLevelQualifiers.NAME, expectedImplicitFileLevelQualifier() );
		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithHbmXmlImplicitFileLevelQualifiers.NAME, expectedImplicitFileLevelQualifier() );
		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithOrmXmlNoFileLevelQualifiers.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithHbmXmlNoFileLevelQualifiers.NAME, expectedDefaultQualifier() );

		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithJoinedInheritanceWithDefaultQualifiers.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithJoinedInheritanceWithDefaultQualifiersSubclass.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithJoinedInheritanceWithExplicitQualifiers.NAME, expectedExplicitQualifier() );
		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithJoinedInheritanceWithExplicitQualifiersSubclass.NAME, expectedExplicitQualifier() );

		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithTablePerClassInheritanceWithDefaultQualifiers.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithTablePerClassInheritanceWithDefaultQualifiersSubclass.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithTablePerClassInheritanceWithExplicitQualifiers.NAME, expectedExplicitQualifier() );
		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithTablePerClassInheritanceWithExplicitQualifiersSubclass.NAME, expectedExplicitQualifier() );

		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithDefaultQualifiersWithCustomSql.NAME, expectedDefaultQualifier() );

		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithDefaultQualifiersWithIdentityGenerator.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithExplicitQualifiersWithIdentityGenerator.NAME, expectedExplicitQualifier() );
		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithDefaultQualifiersWithTableGenerator.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithExplicitQualifiersWithTableGenerator.NAME, expectedExplicitQualifier() );
		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithDefaultQualifiersWithSequenceGenerator.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithExplicitQualifiersWithSequenceGenerator.NAME, expectedExplicitQualifier() );
		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithDefaultQualifiersWithIncrementGenerator.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithExplicitQualifiersWithIncrementGenerator.NAME, expectedExplicitQualifier() );
		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithDefaultQualifiersWithEnhancedSequenceGenerator.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, SqlType.DDL, EntityWithExplicitQualifiersWithEnhancedSequenceGenerator.NAME, expectedExplicitQualifier() );

		if ( dbSupportsCatalogs && options.expectedDefaultCatalog != null ) {
			verifyOnlyQualifier( sql, SqlType.DDL, "catalogPrefixedAuxObject",
					expectedQualifier( options.expectedDefaultCatalog, null ) );
		}
		if ( dbSupportsSchemas && options.expectedDefaultSchema != null ) {
			verifyOnlyQualifier( sql, SqlType.DDL, "schemaPrefixedAuxObject",
					expectedQualifier( null, options.expectedDefaultSchema ) );
		}
	}

	private enum SqlType {
		DDL,
		RUNTIME
	}

	private void verifyOnlyQualifier(String[] sql, SqlType sqlType, String name, ExpectedQualifier expectedQualifier) {
		verifyOnlyQualifier( String.join( "\n", sql ), sqlType, name, expectedQualifier );
	}

	private void verifyOnlyQualifierOptional(String sql, SqlType sqlType, String name, ExpectedQualifier expectedQualifier) {
		verifyOnlyQualifier( sql, sqlType, name, expectedQualifier, true );
	}

	private void verifyOnlyQualifier(String sql, SqlType sqlType, String name, ExpectedQualifier expectedQualifier) {
		verifyOnlyQualifier( sql, sqlType, name, expectedQualifier, false );
	}

	private void verifyOnlyQualifier(String sql, SqlType sqlType, String name, ExpectedQualifier expectedQualifier, boolean optional) {
		String patternStringForTableName = SQL_QUOTE_CHARACTER_CLASS + "?" + Pattern.quote( name ) + "(?!\\w*_seq)" + SQL_QUOTE_CHARACTER_CLASS + "?";
		String patternStringForSequenceName = SQL_QUOTE_CHARACTER_CLASS + "?" + Pattern.quote( name ) + "\\w*_seq" + SQL_QUOTE_CHARACTER_CLASS + "?";

		ExpectedQualifier expectedQualifierForTables = expectedQualifier;
		ExpectedQualifier expectedQualifierForSequences;
		if ( SqlType.DDL == sqlType && factoryScope.getSessionFactory().getJdbcServices().getDialect() instanceof SQLServerDialect ) {
			// SQL Server does not allow the catalog in the sequence name when creating the sequence,
			// so we need different patterns for sequence names and table names.
			// See org.hibernate.dialect.SQLServer2012Dialect.SqlServerSequenceExporter.getFormattedSequenceName
			expectedQualifierForSequences = new ExpectedQualifier( null, expectedQualifier.schema );
		}
		else {
			expectedQualifierForSequences = expectedQualifier;
		}

		if ( !optional ) {
			// Check that we find the name at least once with the proper qualifier, be it a table or a sequence.
			// While not strictly necessary, this ensures our patterns are not completely wrong.
			assertThat( sql )
					.containsPattern(
							"(" + expectedQualifierForTables.patternStringForNameWithThisQualifier( patternStringForTableName )
									+ ")|("
									+ expectedQualifierForSequences.patternStringForNameWithThisQualifier( patternStringForSequenceName )
									+ ")" );
		}

		// Check that we don't find any name with an incorrect qualifier
		assertThat( sql.split( System.getProperty( "line.separator" ) ) )
				.allSatisfy( line -> assertThat( line )
						.doesNotContainPattern( expectedQualifierForTables
								.patternStringForNameWithDifferentQualifier( patternStringForTableName ) )
						.doesNotContainPattern( expectedQualifierForSequences
								.patternStringForNameWithDifferentQualifier( patternStringForSequenceName ) ) );
	}

	private ExpectedQualifier expectedDefaultQualifier() {
		return expectedQualifier( options.expectedDefaultCatalog, options.expectedDefaultSchema );
	}

	private ExpectedQualifier expectedExplicitQualifier() {
		return expectedQualifier( EXPLICIT_CATALOG, EXPLICIT_SCHEMA );
	}

	private ExpectedQualifier expectedImplicitFileLevelQualifier() {
		return expectedQualifier( IMPLICIT_FILE_LEVEL_CATALOG, IMPLICIT_FILE_LEVEL_SCHEMA );
	}

	private ExpectedQualifier expectedQualifier(String catalog, String schema) {
		return new ExpectedQualifier(
				dbSupportsCatalogs ? catalog : null,
				dbSupportsSchemas ? schema : null
		);
	}

	private String generateScriptFromSessionFactory(
			String action,
			DomainModelScope modelScope,
			SessionFactoryScope factoryScope) {
		var serviceRegistry = factoryScope.getSessionFactory().getServiceRegistry();
		var settings = new HashMap<>(
				serviceRegistry.requireService( ConfigurationService.class ).getSettings()
		);
		StringWriter writer = new StringWriter();
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, action );
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, writer );
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET, writer );

		SchemaManagementToolCoordinator.process(
				modelScope.getDomainModel(),
				serviceRegistry,
				settings,
				DelayedDropRegistryNotAvailableImpl.INSTANCE
		);
		return writer.toString();
	}

	// This is precisely how scripts are generated for the Quarkus DevUI
	// Don't change this code except to match changes in
	// https://github.com/quarkusio/quarkus/blob/d07ecb23bfba38ee48868635e155c4b513ce6af9/extensions/hibernate-orm/runtime/src/main/java/io/quarkus/hibernate/orm/runtime/devconsole/HibernateOrmDevConsoleInfoSupplier.java#L61-L92
	private String generateScriptFromMetadata(
			SchemaExport.Action action,
			DomainModelScope modelScope) {
		ServiceRegistryImplementor serviceRegistry = factoryScope.getSessionFactory().getServiceRegistry();
		SchemaExport schemaExport = new SchemaExport();
		schemaExport.setFormat( true );
		schemaExport.setDelimiter( ";" );
		StringWriter writer = new StringWriter();
		schemaExport.doExecution( action, false, modelScope.getDomainModel(), serviceRegistry,
				new TargetDescriptor() {
					@Override
					public EnumSet<TargetType> getTargetTypes() {
						return EnumSet.of( TargetType.SCRIPT );
					}

					@Override
					public ScriptTargetOutput getScriptTargetOutput() {
						return new ScriptTargetOutputToWriter( writer ) {
							@Override
							public void accept(String command) {
								super.accept( command );
							}
						};
					}
				}
		);
		return writer.toString();
	}

	private static class ExpectedQualifier {
		private final String catalog;
		private final String schema;

		private ExpectedQualifier(String catalog, String schema) {
			this.catalog = catalog;
			this.schema = schema;
		}

		String patternStringForNameWithThisQualifier(String patternStringForName) {
			if ( catalog == null && schema == null ) {
				// Look for unqualified names
				return "(?<!\\.)" + patternStringForName;
			}
			else {
				// Look for a qualified name with this exact qualifier
				return "(?<!\\.)" + patternStringForQualifier() + patternStringForName;
			}
		}

		String patternStringForNameWithDifferentQualifier(String patternStringForName) {
			if ( catalog == null && schema == null ) {
				// Look for a qualified name with any qualifier
				return "\\." + patternStringForName;
			}
			else {
				// Look for a qualified name with a different qualifier
				// ignoring content of string literals (preceded with a single-quote)
				return "(?<!" + patternStringForQualifier() + "|')" + patternStringForName;
			}
		}

		private String patternStringForQualifier() {
			return ( catalog != null ? Pattern.quote( catalog ) + "." : "" )
					+ ( schema != null ? Pattern.quote( schema ) + "." : "" );
		}
	}

	@Entity(name = EntityWithDefaultQualifiers.NAME)
	public static class EntityWithDefaultQualifiers {
		public static final String NAME = "EntityWithDefaultQualifiers";
		@Id
		private Long id;
		@Basic
		private String basic;
		@OneToMany
		@JoinTable(name = NAME + "_oneToMany",
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				inverseJoinColumns = @JoinColumn(name = "inverse"),
				foreignKey = @ForeignKey(name = "FK_oneToMany"))
		private List<EntityWithDefaultQualifiers> oneToMany;
		@OneToMany
		@JoinTable(name = NAME + "_manyToMany",
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				inverseJoinColumns = @JoinColumn(name = "inverse"),
				foreignKey = @ForeignKey(name = "FK_manyToMany"))
		private List<EntityWithDefaultQualifiers> manyToMany;
		@ElementCollection
		@JoinTable(name = NAME + "_elementCollection",
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				foreignKey = @ForeignKey(name = "FK_elementCollection"))
		private List<String> elementCollection;
	}

	@Entity(name = EntityWithExplicitQualifiers.NAME)
	@Table(catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA)
	public static class EntityWithExplicitQualifiers {
		public static final String NAME = "EntityWithExplicitQualifiers";
		@Id
		private Long id;
		@Basic
		private String basic;
		@OneToMany
		@JoinTable(name = NAME + "_oneToMany", catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA,
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				inverseJoinColumns = @JoinColumn(name = "inverse"),
				foreignKey = @ForeignKey(name = "FK_oneToMany"))
		private List<EntityWithDefaultQualifiers> oneToMany;
		@OneToMany
		@JoinTable(name = NAME + "_manyToMany", catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA,
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				inverseJoinColumns = @JoinColumn(name = "inverse"),
				foreignKey = @ForeignKey(name = "FK_manyToMany"))
		private List<EntityWithDefaultQualifiers> manyToMany;
		@ElementCollection
		@JoinTable(name = NAME + "_elementCollection", catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA,
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				foreignKey = @ForeignKey(name = "FK_elementCollection"))
		private List<String> elementCollection;
	}

	public static class EntityWithOrmXmlImplicitFileLevelQualifiers {
		public static final String NAME = "EntityWithOrmXmlImplicitFileLevelQualifiers";
		private Long id;
		private String basic;
		private List<EntityWithDefaultQualifiers> oneToMany;
		private List<EntityWithDefaultQualifiers> manyToMany;
		private List<String> elementCollection;
	}

	public static class EntityWithHbmXmlImplicitFileLevelQualifiers {
		public static final String NAME = "EntityWithHbmXmlImplicitFileLevelQualifiers";
		private Long id;
		private String basic;
	}

	public static class EntityWithOrmXmlNoFileLevelQualifiers {
		public static final String NAME = "EntityWithOrmXmlNoFileLevelQualifiers";
		private Long id;
		private String basic;
		private List<EntityWithDefaultQualifiers> oneToMany;
		private List<EntityWithDefaultQualifiers> manyToMany;
		private List<String> elementCollection;
	}

	public static class EntityWithHbmXmlNoFileLevelQualifiers {
		public static final String NAME = "EntityWithHbmXmlNoFileLevelQualifiers";
		private Long id;
		private String basic;
	}

	@Entity(name = EntityWithJoinedInheritanceWithDefaultQualifiers.NAME)
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class EntityWithJoinedInheritanceWithDefaultQualifiers {
		public static final String NAME = "EntityWithJoinedInheritanceWithDefaultQualifiers";
		@Id
		private Long id;
		@Basic
		private String basic;
		@OneToMany
		@JoinTable(name = NAME + "_oneToMany",
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				inverseJoinColumns = @JoinColumn(name = "inverse"),
				foreignKey = @ForeignKey(name = "FK_oneToMany"))
		private List<EntityWithDefaultQualifiers> oneToMany;
		@OneToMany
		@JoinTable(name = NAME + "_manyToMany",
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				inverseJoinColumns = @JoinColumn(name = "inverse"),
				foreignKey = @ForeignKey(name = "FK_manyToMany"))
		private List<EntityWithDefaultQualifiers> manyToMany;
		@ElementCollection
		@JoinTable(name = NAME + "_elementCollection",
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				foreignKey = @ForeignKey(name = "FK_elementCollection"))
		private List<String> elementCollection;
	}

	@Entity(name = EntityWithJoinedInheritanceWithDefaultQualifiersSubclass.NAME)
	public static class EntityWithJoinedInheritanceWithDefaultQualifiersSubclass
			extends EntityWithJoinedInheritanceWithDefaultQualifiers {
		public static final String NAME = "EntityWithJoinedInheritanceWithDefaultQualifiersSubclass";
		@Basic
		private String basic2;
	}

	@Entity(name = EntityWithJoinedInheritanceWithExplicitQualifiers.NAME)
	@Inheritance(strategy = InheritanceType.JOINED)
	@Table(catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA)
	public static class EntityWithJoinedInheritanceWithExplicitQualifiers {
		public static final String NAME = "EntityWithJoinedInheritanceWithExplicitQualifiers";
		@Id
		private Long id;
		@Basic
		private String basic;
		@OneToMany
		@JoinTable(name = NAME + "_oneToMany", catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA,
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				inverseJoinColumns = @JoinColumn(name = "inverse"),
				foreignKey = @ForeignKey(name = "FK_oneToMany"))
		private List<EntityWithDefaultQualifiers> oneToMany;
		@OneToMany
		@JoinTable(name = NAME + "_manyToMany", catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA,
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				inverseJoinColumns = @JoinColumn(name = "inverse"),
				foreignKey = @ForeignKey(name = "FK_manyToMany"))
		private List<EntityWithDefaultQualifiers> manyToMany;
		@ElementCollection
		@JoinTable(name = NAME + "_elementCollection", catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA,
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				foreignKey = @ForeignKey(name = "FK_elementCollection"))
		private List<String> elementCollection;
	}

	@Entity(name = EntityWithJoinedInheritanceWithExplicitQualifiersSubclass.NAME)
	@Table(catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA)
	public static class EntityWithJoinedInheritanceWithExplicitQualifiersSubclass
			extends EntityWithJoinedInheritanceWithExplicitQualifiers {
		public static final String NAME = "EntityWithJoinedInheritanceWithExplicitQualifiersSubclass";
		@Basic
		private String basic2;
	}

	@Entity(name = EntityWithTablePerClassInheritanceWithDefaultQualifiers.NAME)
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	public static class EntityWithTablePerClassInheritanceWithDefaultQualifiers {
		public static final String NAME = "EntityWithTablePerClassInheritanceWithDefaultQualifiers";
		@Id
		private Long id;
		@Basic
		private String basic;
		@OneToMany
		@JoinTable(name = NAME + "_oneToMany",
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				inverseJoinColumns = @JoinColumn(name = "inverse"),
				foreignKey = @ForeignKey(name = "FK_oneToMany"))
		private List<EntityWithDefaultQualifiers> oneToMany;
		@OneToMany
		@JoinTable(name = NAME + "_manyToMany",
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				inverseJoinColumns = @JoinColumn(name = "inverse"),
				foreignKey = @ForeignKey(name = "FK_manyToMany"))
		private List<EntityWithDefaultQualifiers> manyToMany;
		@ElementCollection
		@JoinTable(name = NAME + "_elementCollection",
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				foreignKey = @ForeignKey(name = "FK_elementCollection"))
		private List<String> elementCollection;
	}

	@Entity(name = EntityWithTablePerClassInheritanceWithDefaultQualifiersSubclass.NAME)
	public static class EntityWithTablePerClassInheritanceWithDefaultQualifiersSubclass
			extends EntityWithTablePerClassInheritanceWithDefaultQualifiers {
		public static final String NAME = "EntityWithTablePerClassInheritanceWithDefaultQualifiersSubclass";
		@Basic
		private String basic2;
	}

	@Entity(name = EntityWithTablePerClassInheritanceWithExplicitQualifiers.NAME)
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	@Table(catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA)
	public static class EntityWithTablePerClassInheritanceWithExplicitQualifiers {
		public static final String NAME = "EntityWithTablePerClassInheritanceWithExplicitQualifiers";
		@Id
		private Long id;
		@Basic
		private String basic;
		@OneToMany
		@JoinTable(name = NAME + "_oneToMany", catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA,
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				inverseJoinColumns = @JoinColumn(name = "inverse"),
				foreignKey = @ForeignKey(name = "FK_oneToMany"))
		private List<EntityWithDefaultQualifiers> oneToMany;
		@OneToMany
		@JoinTable(name = NAME + "_manyToMany", catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA,
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				inverseJoinColumns = @JoinColumn(name = "inverse"),
				foreignKey = @ForeignKey(name = "FK_manyToMany"))
		private List<EntityWithDefaultQualifiers> manyToMany;
		@ElementCollection
		@JoinTable(name = NAME + "_elementCollection", catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA,
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				foreignKey = @ForeignKey(name = "FK_elementCollection"))
		private List<String> elementCollection;
	}

	@Entity(name = EntityWithTablePerClassInheritanceWithExplicitQualifiersSubclass.NAME)
	@Table(catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA)
	public static class EntityWithTablePerClassInheritanceWithExplicitQualifiersSubclass
			extends EntityWithTablePerClassInheritanceWithExplicitQualifiers {
		public static final String NAME = "EntityWithTablePerClassInheritanceWithExplicitQualifiersSubclass";
		@Basic
		private String basic2;
	}

	@Entity(name = EntityWithDefaultQualifiersWithCustomSql.NAME)
	@SQLInsert(sql = CUSTOM_INSERT_SQL_PART_1 + EntityWithDefaultQualifiersWithCustomSql.NAME + CUSTOM_INSERT_SQL_PART_2
			+ EntityWithDefaultQualifiersWithCustomSql.NAME + CUSTOM_INSERT_SQL_PART_3)
	@SQLUpdate(sql = CUSTOM_UPDATE_SQL_PART_1 + EntityWithDefaultQualifiersWithCustomSql.NAME + CUSTOM_UPDATE_SQL_PART_2
			+ EntityWithDefaultQualifiersWithCustomSql.NAME + CUSTOM_UPDATE_SQL_PART_3)
	@SQLDelete(sql = CUSTOM_DELETE_SQL_PART_1 + EntityWithDefaultQualifiersWithCustomSql.NAME + CUSTOM_DELETE_SQL_PART_2
			+ EntityWithDefaultQualifiersWithCustomSql.NAME + CUSTOM_DELETE_SQL_PART_3)
	public static class EntityWithDefaultQualifiersWithCustomSql {
		public static final String NAME = "EntityWithDefaultQualifiersWithCustomSql";
		@Id
		private Long id;
		@Basic
		private String basic;
		@OneToMany
		@JoinTable(name = NAME + "_oneToMany",
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				inverseJoinColumns = @JoinColumn(name = "inverse"),
				foreignKey = @ForeignKey(name = "FK_oneToMany"))
		@SQLInsert(sql = CUSTOM_INSERT_SQL_PART_1 + EntityWithDefaultQualifiersWithCustomSql.NAME + "_oneToMany" + CUSTOM_INSERT_SQL_PART_2
				+ EntityWithDefaultQualifiersWithCustomSql.NAME + "_oneToMany" + CUSTOM_INSERT_SQL_PART_3)
		@SQLUpdate(sql = CUSTOM_UPDATE_SQL_PART_1 + EntityWithDefaultQualifiersWithCustomSql.NAME + "_oneToMany" + CUSTOM_UPDATE_SQL_PART_2
				+ EntityWithDefaultQualifiersWithCustomSql.NAME + "_oneToMany" + CUSTOM_UPDATE_SQL_PART_3)
		@SQLDelete(sql = CUSTOM_DELETE_SQL_PART_1 + EntityWithDefaultQualifiersWithCustomSql.NAME + "_oneToMany" + CUSTOM_DELETE_SQL_PART_2
				+ EntityWithDefaultQualifiersWithCustomSql.NAME + "_oneToMany" + CUSTOM_DELETE_SQL_PART_3)
		private List<EntityWithDefaultQualifiers> oneToMany;
		@OneToMany
		@JoinTable(name = NAME + "_manyToMany",
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				inverseJoinColumns = @JoinColumn(name = "inverse"),
				foreignKey = @ForeignKey(name = "FK_manyToMany"))
		@SQLInsert(sql = CUSTOM_INSERT_SQL_PART_1 + EntityWithDefaultQualifiersWithCustomSql.NAME + "_manyToMany" + CUSTOM_INSERT_SQL_PART_2
				+ EntityWithDefaultQualifiersWithCustomSql.NAME + "_manyToMany" + CUSTOM_INSERT_SQL_PART_3)
		@SQLUpdate(sql = CUSTOM_UPDATE_SQL_PART_1 + EntityWithDefaultQualifiersWithCustomSql.NAME + "_manyToMany" + CUSTOM_UPDATE_SQL_PART_2
				+ EntityWithDefaultQualifiersWithCustomSql.NAME + "_manyToMany" + CUSTOM_UPDATE_SQL_PART_3)
		@SQLDelete(sql = CUSTOM_DELETE_SQL_PART_1 + EntityWithDefaultQualifiersWithCustomSql.NAME + "_manyToMany" + CUSTOM_DELETE_SQL_PART_2
				+ EntityWithDefaultQualifiersWithCustomSql.NAME + "_manyToMany" + CUSTOM_DELETE_SQL_PART_3)
		private List<EntityWithDefaultQualifiers> manyToMany;
		@ElementCollection
		@JoinTable(name = NAME + "_elementCollection",
				// Custom names to avoid false positive in assertions
				joinColumns = @JoinColumn(name = "forward"),
				foreignKey = @ForeignKey(name = "FK_elementCollection"))
		@SQLInsert(sql = CUSTOM_INSERT_SQL_PART_1 + EntityWithDefaultQualifiersWithCustomSql.NAME + "_elementCollection" + CUSTOM_INSERT_SQL_PART_2
				+ EntityWithDefaultQualifiersWithCustomSql.NAME + "_elementCollection" + CUSTOM_INSERT_SQL_PART_3)
		@SQLUpdate(sql = CUSTOM_UPDATE_SQL_PART_1 + EntityWithDefaultQualifiersWithCustomSql.NAME + "_elementCollection" + CUSTOM_UPDATE_SQL_PART_2
				+ EntityWithDefaultQualifiersWithCustomSql.NAME + "_elementCollection" + CUSTOM_UPDATE_SQL_PART_3)
		@SQLDelete(sql = CUSTOM_DELETE_SQL_PART_1 + EntityWithDefaultQualifiersWithCustomSql.NAME + "_elementCollection" + CUSTOM_DELETE_SQL_PART_2
				+ EntityWithDefaultQualifiersWithCustomSql.NAME + "_elementCollection" + CUSTOM_DELETE_SQL_PART_3)
		private List<String> elementCollection;
	}

	@Entity(name = EntityWithDefaultQualifiersWithIdentityGenerator.NAME)
	public static class EntityWithDefaultQualifiersWithIdentityGenerator {
		public static final String NAME = "EntityWithDefaultQualifiersWithIdentityGenerator";
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
		@Basic
		private String basic;
	}

	@Entity(name = EntityWithExplicitQualifiersWithIdentityGenerator.NAME)
	@Table(catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA)
	public static class EntityWithExplicitQualifiersWithIdentityGenerator {
		public static final String NAME = "EntityWithExplicitQualifiersWithIdentityGenerator";
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
		@Basic
		private String basic;
	}

	@Entity(name = EntityWithDefaultQualifiersWithSequenceGenerator.NAME)
	public static class EntityWithDefaultQualifiersWithSequenceGenerator {
		public static final String NAME = "EntityWithDefaultQualifiersWithSequenceGenerator";
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = NAME + "_generator")
		@SequenceGenerator(name = NAME + "_generator", sequenceName = NAME + "_seq")
		private Long id;
		@Basic
		private String basic;
	}

	@Entity(name = EntityWithExplicitQualifiersWithSequenceGenerator.NAME)
	@Table(catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA)
	public static class EntityWithExplicitQualifiersWithSequenceGenerator {
		public static final String NAME = "EntityWithExplicitQualifiersWithSequenceGenerator";
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = NAME + "_generator")
		@SequenceGenerator(name = NAME + "_generator", sequenceName = NAME + "_seq",
				catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA)
		private Long id;
		@Basic
		private String basic;
	}


	@Entity(name = EntityWithDefaultQualifiersWithTableGenerator.NAME)
	public static class EntityWithDefaultQualifiersWithTableGenerator {
		public static final String NAME = "EntityWithDefaultQualifiersWithTableGenerator";
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = NAME + "_generator")
		@TableGenerator(name = NAME + "_generator", table = NAME + "_tableseq")
		private Long id;
		@Basic
		private String basic;
	}

	@Entity(name = EntityWithExplicitQualifiersWithTableGenerator.NAME)
	@Table(catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA)
	public static class EntityWithExplicitQualifiersWithTableGenerator {
		public static final String NAME = "EntityWithExplicitQualifiersWithTableGenerator";
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE, generator = NAME + "_generator")
		@TableGenerator(name = NAME + "_generator", table = NAME + "_tableseq",
				catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA)
		private Long id;
		@Basic
		private String basic;
	}


	@Entity(name = EntityWithDefaultQualifiersWithIncrementGenerator.NAME)
	public static class EntityWithDefaultQualifiersWithIncrementGenerator {
		public static final String NAME = "EntityWithDefaultQualifiersWithIncrementGenerator";
		@Id
		@GeneratedValue(generator = NAME + "_generator")
		@GenericGenerator(name = NAME + "_generator", strategy = "increment")
		private Long id;
		@Basic
		private String basic;
	}

	@Entity(name = EntityWithExplicitQualifiersWithIncrementGenerator.NAME)
	@Table(catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA)
	public static class EntityWithExplicitQualifiersWithIncrementGenerator {
		public static final String NAME = "EntityWithExplicitQualifiersWithIncrementGenerator";
		@Id
		@GeneratedValue(generator = NAME + "_generator")
		@GenericGenerator(name = NAME + "_generator", strategy = "increment", parameters = {
				@Parameter(name = "catalog", value = EXPLICIT_CATALOG),
				@Parameter(name = "schema", value = EXPLICIT_SCHEMA)
		})
		private Long id;
		@Basic
		private String basic;
	}

	@Entity(name = EntityWithDefaultQualifiersWithEnhancedSequenceGenerator.NAME)
	public static class EntityWithDefaultQualifiersWithEnhancedSequenceGenerator {
		public static final String NAME = "EntityWithDefaultQualifiersWithEnhancedSequenceGenerator";
		@Id
		@GeneratedValue(generator = NAME + "_generator")
		@GenericGenerator(name = NAME + "_generator", strategy = "enhanced-sequence", parameters = {
				@Parameter(name = "sequence_name", value = NAME + "_seq")
		})
		private Long id;
		@Basic
		private String basic;
	}

	@Entity(name = EntityWithExplicitQualifiersWithEnhancedSequenceGenerator.NAME)
	@Table(catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA)
	public static class EntityWithExplicitQualifiersWithEnhancedSequenceGenerator {
		public static final String NAME = "EntityWithExplicitQualifiersWithEnhancedSequenceGenerator";
		@Id
		@GeneratedValue(generator = NAME + "_generator")
		@GenericGenerator(name = NAME + "_generator", strategy = "enhanced-sequence", parameters = {
				@Parameter(name = "sequence_name", value = NAME + "_seq"),
				@Parameter(name = "catalog", value = EXPLICIT_CATALOG),
				@Parameter(name = "schema", value = EXPLICIT_SCHEMA)
		})
		private Long id;
		@Basic
		private String basic;
	}

}
