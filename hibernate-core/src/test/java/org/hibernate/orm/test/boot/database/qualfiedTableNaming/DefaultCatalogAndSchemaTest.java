/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.database.qualfiedTableNaming;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.internal.SessionFactoryBuilderImpl;
import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
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
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToWriter;
import org.hibernate.tool.schema.spi.DelayedDropRegistryNotAvailableImpl;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.CustomParameterized;
import org.hibernate.testing.orm.junit.JiraKeyGroup;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(CustomParameterized.class)
@JiraKeyGroup( value = {
		@JiraKey( value = "HHH-14921" ),
		@JiraKey( value = "HHH-14922" ),
		@JiraKey( value = "HHH-15212" ),
		@JiraKey( value = "HHH-16177" )
} )
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
public class DefaultCatalogAndSchemaTest {

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

	@Parameterized.Parameters(name = "settingsMode = {0}, configuredXmlMappingPath = {1}, configuredDefaultCatalog = {2}, configuredDefaultSchema = {3}")
	public static List<Object[]> params() {
		List<Object[]> params = new ArrayList<>();
		for ( SettingsMode mode : SettingsMode.values() ) {
			for ( String defaultCatalog : Arrays.asList( null, "someDefaultCatalog" ) ) {
				for ( String defaultSchema : Arrays.asList( null, "someDefaultSchema" ) ) {
					params.add( new Object[] { mode, null, defaultCatalog, defaultSchema,
							// The default catalog/schema should be used when
							// there is no implicit catalog/schema defined in the mapping.
							defaultCatalog, defaultSchema  } );
				}
			}
			params.add( new Object[] { mode, "implicit-global-catalog-and-schema.orm.xml",
					null, null,
					"someImplicitCatalog", "someImplicitSchema"  } );
			// HHH-14922: Inconsistent precedence of orm.xml implicit catalog/schema over "default_catalog"/"default_schema"
			params.add( new Object[] { mode, "implicit-global-catalog-and-schema.orm.xml",
					"someDefaultCatalog", "someDefaultSchema",
					// The default catalog/schema should replace the
					// implicit catalog/schema defined in the mapping.
					"someDefaultCatalog", "someDefaultSchema"  } );
		}
		return params;
	}

	@Parameterized.Parameter
	public SettingsMode settingsMode;
	@Parameterized.Parameter(1)
	public String configuredXmlMappingPath;
	@Parameterized.Parameter(2)
	public String configuredDefaultCatalog;
	@Parameterized.Parameter(3)
	public String configuredDefaultSchema;
	@Parameterized.Parameter(4)
	public String expectedDefaultCatalog;
	@Parameterized.Parameter(5)
	public String expectedDefaultSchema;

	private boolean dbSupportsCatalogs;
	private boolean dbSupportsSchemas;

	private MetadataImplementor metadata;
	private final List<AutoCloseable> toClose = new ArrayList<>();
	private SessionFactoryImplementor sessionFactory;

	@BeforeClassOnce
	public void initSessionFactory() {
		List<Class<?>> annotatedClasses = Arrays.asList(
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

		StandardServiceRegistry serviceRegistry;
		switch ( settingsMode ) {
			case METADATA_SERVICE_REGISTRY:
				serviceRegistry = createStandardServiceRegistry( configuredDefaultCatalog, configuredDefaultSchema );
				break;
			case SESSION_FACTORY_SERVICE_REGISTRY:
				serviceRegistry = createStandardServiceRegistry( null, null );
				break;
			default:
				throw new IllegalStateException( "Unknown settings mode: " + settingsMode );
		}

		final MetadataSources metadataSources = new MetadataSources( serviceRegistry );
		metadataSources.addInputStream( getClass().getResourceAsStream( "implicit-file-level-catalog-and-schema.orm.xml" ) );
		metadataSources.addInputStream( getClass().getResourceAsStream( "implicit-file-level-catalog-and-schema.hbm.xml" ) );
		metadataSources.addInputStream( getClass().getResourceAsStream( "no-file-level-catalog-and-schema.orm.xml" ) );
		metadataSources.addInputStream( getClass().getResourceAsStream( "no-file-level-catalog-and-schema.hbm.xml" ) );
		metadataSources.addInputStream( getClass().getResourceAsStream( "database-object-using-catalog-placeholder.hbm.xml" ) );
		metadataSources.addInputStream( getClass().getResourceAsStream( "database-object-using-schema-placeholder.hbm.xml" ) );
		if ( configuredXmlMappingPath != null ) {
			metadataSources.addInputStream( getClass().getResourceAsStream( configuredXmlMappingPath ) );
		}
		for ( Class<?> annotatedClass : annotatedClasses ) {
			metadataSources.addAnnotatedClass( annotatedClass );
		}

		final MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder();
		metadata = (MetadataImplementor) metadataBuilder.build();

		SessionFactoryBuilder sfb;
		switch ( settingsMode ) {
			case METADATA_SERVICE_REGISTRY:
				sfb = metadata.getSessionFactoryBuilder();
				break;
			case SESSION_FACTORY_SERVICE_REGISTRY:
				serviceRegistry = createStandardServiceRegistry( configuredDefaultCatalog, configuredDefaultSchema );
				BootstrapContext bootstrapContext = ((MetadataImpl) metadata).getBootstrapContext();
				sfb = new SessionFactoryBuilderImpl(
						metadata,
						new SessionFactoryOptionsBuilder( serviceRegistry, bootstrapContext ),
						bootstrapContext
				);
				break;
			default:
				throw new IllegalStateException( "Unknown settings mode: " + settingsMode );
		}

		sessionFactory = (SessionFactoryImplementor) sfb.build();
		toClose.add( sessionFactory );

		NameQualifierSupport nameQualifierSupport =
				sessionFactory.getJdbcServices().getJdbcEnvironment().getNameQualifierSupport();
		dbSupportsCatalogs = nameQualifierSupport.supportsCatalogs();
		dbSupportsSchemas = nameQualifierSupport.supportsSchemas();
	}

	@AfterClassOnce
	public void cleanup() throws Throwable {
		Throwable thrown = null;
		Collections.reverse( toClose );
		for ( AutoCloseable closeable : toClose ) {
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
	}

	private StandardServiceRegistry createStandardServiceRegistry(String defaultCatalog, String defaultSchema) {
		final BootstrapServiceRegistryBuilder bsrb = new BootstrapServiceRegistryBuilder();
		bsrb.applyClassLoader( getClass().getClassLoader() );
		// by default we do not share the BootstrapServiceRegistry nor the StandardServiceRegistry,
		// so we want the BootstrapServiceRegistry to be automatically closed when the
		// StandardServiceRegistry is closed.
		bsrb.enableAutoClose();

		final BootstrapServiceRegistry bsr = bsrb.build();

		final Map<String, Object> settings = new HashMap<>();
		settings.put( PersistentTableStrategy.DROP_ID_TABLES, "true" );
		settings.put( GlobalTemporaryTableStrategy.DROP_ID_TABLES, "true" );
		settings.put( LocalTemporaryTableStrategy.DROP_ID_TABLES, "true" );
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS, "true" );
		if ( defaultCatalog != null ) {
			settings.put( AvailableSettings.DEFAULT_CATALOG, defaultCatalog );
		}
		if ( defaultSchema != null ) {
			settings.put( AvailableSettings.DEFAULT_SCHEMA, defaultSchema );
		}

		final StandardServiceRegistryBuilder ssrb = ServiceRegistryUtil.serviceRegistryBuilder( bsr );
		ssrb.applySettings( settings );
		StandardServiceRegistry registry = ssrb.build();
		toClose.add( registry );
		return registry;
	}

	@Test
	public void createSchema_fromSessionFactory() {
		String script = generateScriptFromSessionFactory( "create" );
		verifyDDLCreateCatalogOrSchema( script );
		verifyDDLQualifiers( script );
	}

	@Test
	@SkipForDialect(value = SQLServerDialect.class,
			comment = "SQL Server support catalogs but their implementation of DatabaseMetaData"
					+ " throws exceptions when calling getSchemas/getTables with a non-existing catalog,"
					+ " which results in nasty errors when generating an update script"
					+ " and some catalogs don't exist.")
	@SkipForDialect(value = SybaseDialect.class,
			comment = "Sybase support catalogs but their implementation of DatabaseMetaData"
					+ " throws exceptions when calling getSchemas/getTables with a non-existing catalog,"
					+ " which results in nasty errors when generating an update script"
					+ " and some catalogs don't exist.")
	@SkipForDialect(value = InformixDialect.class,
			comment = "Informix support catalogs but their implementation of DatabaseMetaData"
					+ " throws exceptions when calling getSchemas/getTables with a non-existing catalog,"
					+ " which results in nasty errors when generating an update script"
					+ " and some catalogs don't exist.")
	public void updateSchema_fromSessionFactory() {
		String script = generateScriptFromSessionFactory( "update" );
		verifyDDLCreateCatalogOrSchema( script );
		verifyDDLQualifiers( script );
	}

	@Test
	public void dropSchema_fromSessionFactory() {
		String script = generateScriptFromSessionFactory( "drop" );
		verifyDDLDropCatalogOrSchema( script );
		verifyDDLQualifiers( script );
	}

	@Test
	public void createSchema_fromMetadata() {
		String script = generateScriptFromMetadata( SchemaExport.Action.CREATE );
		verifyDDLCreateCatalogOrSchema( script );
		verifyDDLQualifiers( script );
	}

	@Test
	public void dropSchema_fromMetadata() {
		String script = generateScriptFromMetadata( SchemaExport.Action.DROP );
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

	private void verifyEntityPersisterQualifiers(Class<?> entityClass, ExpectedQualifier expectedQualifier) {
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
		final AbstractEntityPersister persister = (AbstractEntityPersister) sessionFactory.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( entityClass );
		return expectedType.cast( persister.getIdentifierGenerator() );
	}

	private void verifyDDLCreateCatalogOrSchema(String sql) {
		Dialect dialect = sessionFactory.getJdbcServices().getDialect();

		if ( sessionFactory.getJdbcServices().getDialect().canCreateCatalog() ) {
			assertThat( sql ).contains( dialect.getCreateCatalogCommand( EXPLICIT_CATALOG ) );
			assertThat( sql ).contains( dialect.getCreateCatalogCommand( IMPLICIT_FILE_LEVEL_CATALOG ) );
			if ( expectedDefaultCatalog != null ) {
				assertThat( sql ).contains( dialect.getCreateCatalogCommand( expectedDefaultCatalog ) );
			}
		}

		if ( sessionFactory.getJdbcServices().getDialect().canCreateSchema() ) {
			assertThat( sql ).contains( dialect.getCreateSchemaCommand( EXPLICIT_SCHEMA ) );
			assertThat( sql ).contains( dialect.getCreateSchemaCommand( IMPLICIT_FILE_LEVEL_SCHEMA ) );
			if ( expectedDefaultSchema != null ) {
				assertThat( sql ).contains( dialect.getCreateSchemaCommand( expectedDefaultSchema ) );
			}
		}
	}

	private void verifyDDLDropCatalogOrSchema(String sql) {
		Dialect dialect = sessionFactory.getJdbcServices().getDialect();

		if ( sessionFactory.getJdbcServices().getDialect().canCreateCatalog() ) {
			assertThat( sql ).contains( dialect.getDropCatalogCommand( EXPLICIT_CATALOG ) );
			assertThat( sql ).contains( dialect.getDropCatalogCommand( IMPLICIT_FILE_LEVEL_CATALOG ) );
			if ( expectedDefaultCatalog != null ) {
				assertThat( sql ).contains( dialect.getDropCatalogCommand( expectedDefaultCatalog ) );
			}
		}

		if ( sessionFactory.getJdbcServices().getDialect().canCreateSchema() ) {
			assertThat( sql ).contains( dialect.getDropSchemaCommand( EXPLICIT_SCHEMA ) );
			assertThat( sql ).contains( dialect.getDropSchemaCommand( IMPLICIT_FILE_LEVEL_SCHEMA ) );
			if ( expectedDefaultSchema != null ) {
				assertThat( sql ).contains( dialect.getDropSchemaCommand( expectedDefaultSchema ) );
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

		if ( dbSupportsCatalogs && expectedDefaultCatalog != null ) {
			verifyOnlyQualifier( sql, SqlType.DDL, "catalogPrefixedAuxObject",
					expectedQualifier( expectedDefaultCatalog, null ) );
		}
		if ( dbSupportsSchemas && expectedDefaultSchema != null ) {
			verifyOnlyQualifier( sql, SqlType.DDL, "schemaPrefixedAuxObject",
					expectedQualifier( null, expectedDefaultSchema ) );
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
		if ( SqlType.DDL == sqlType && sessionFactory.getJdbcServices().getDialect() instanceof SQLServerDialect ) {
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
		return expectedQualifier( expectedDefaultCatalog, expectedDefaultSchema );
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

	private String generateScriptFromSessionFactory(String action) {
		ServiceRegistryImplementor serviceRegistry = sessionFactory.getServiceRegistry();
		Map<String, Object> settings = new HashMap<>(
				serviceRegistry.getService( ConfigurationService.class ).getSettings()
		);
		StringWriter writer = new StringWriter();
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, action );
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, writer );
		settings.put( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_DROP_TARGET, writer );

		SchemaManagementToolCoordinator.process(
				metadata, serviceRegistry, settings, DelayedDropRegistryNotAvailableImpl.INSTANCE );
		return writer.toString();
	}

	// This is precisely how scripts are generated for the Quarkus DevUI
	// Don't change this code except to match changes in
	// https://github.com/quarkusio/quarkus/blob/d07ecb23bfba38ee48868635e155c4b513ce6af9/extensions/hibernate-orm/runtime/src/main/java/io/quarkus/hibernate/orm/runtime/devconsole/HibernateOrmDevConsoleInfoSupplier.java#L61-L92
	private String generateScriptFromMetadata(SchemaExport.Action action) {
		ServiceRegistryImplementor sessionFactoryServiceRegistry = sessionFactory.getServiceRegistry();
		SchemaExport schemaExport = new SchemaExport();
		schemaExport.setFormat( true );
		schemaExport.setDelimiter( ";" );
		StringWriter writer = new StringWriter();
		schemaExport.doExecution( action, false, metadata, sessionFactoryServiceRegistry,
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
