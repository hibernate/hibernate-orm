/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.boot.database.qualfiedTableNaming;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.persistence.Basic;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

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
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.spi.id.global.GlobalTemporaryTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.LocalTemporaryTableBulkIdStrategy;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.internal.exec.ScriptTargetOutputToWriter;
import org.hibernate.tool.schema.spi.DelayedDropRegistryNotAvailableImpl;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.hibernate.tool.schema.spi.ScriptTargetOutput;
import org.hibernate.tool.schema.spi.TargetDescriptor;

import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SharedDriverManagerConnectionProviderImpl;
import org.hibernate.testing.junit4.CustomParameterized;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(CustomParameterized.class)
@TestForIssue(jiraKey = { "HHH-14921", "HHH-14922" })
public class DefaultCatalogAndSchemaTest {

	private static final String SQL_QUOTE_CHARACTER_CLASS = "([`\"]|\\[|\\])";

	private static final String EXPLICIT_CATALOG = "someExplicitCatalog";
	private static final String EXPLICIT_SCHEMA = "someExplicitSchema";

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
		SESSION_FACTORY_SERVICE_REGISTRY;
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
							defaultCatalog, defaultSchema } );
				}
			}
			params.add( new Object[] { mode, "implicit-global-catalog-and-schema.orm.xml",
					null, null,
					"someImplicitCatalog", "someImplicitSchema" } );
			params.add( new Object[] { mode, "implicit-global-catalog-and-schema.orm.xml",
					"someDefaultCatalog", "someDefaultSchema",
					// The default catalog/schema should replace the
					// implicit catalog/schema defined in the mapping.
					"someDefaultCatalog", "someDefaultSchema" } );
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

	private MetadataImplementor metadata;
	private List<AutoCloseable> toClose = new ArrayList<>();
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
				EntityWithDefaultQualifiersWithSeqHiLoGenerator.class,
				EntityWithExplicitQualifiersWithSeqHiLoGenerator.class,
				EntityWithDefaultQualifiersWithIncrementGenerator.class,
				EntityWithExplicitQualifiersWithIncrementGenerator.class,
				EntityWithDefaultQualifiersWithSequenceIdentityGenerator.class,
				EntityWithExplicitQualifiersWithSequenceIdentityGenerator.class,
				EntityWithDefaultQualifiersWithEnhancedSequenceGenerator.class,
				EntityWithExplicitQualifiersWithEnhancedSequenceGenerator.class,
				EntityWithDefaultQualifiersWithLegacySequenceGenerator.class,
				EntityWithExplicitQualifiersWithLegacySequenceGenerator.class
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
				sfb = new SessionFactoryBuilderImpl( metadata, new SessionFactoryOptionsBuilder( serviceRegistry,
						((MetadataImpl) metadata).getBootstrapContext() ) );
				break;
			default:
				throw new IllegalStateException( "Unknown settings mode: " + settingsMode );
		}

		sessionFactory = (SessionFactoryImplementor) sfb.build();
		toClose.add( sessionFactory );
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
		settings.put( GlobalTemporaryTableBulkIdStrategy.DROP_ID_TABLES, "true" );
		settings.put( LocalTemporaryTableBulkIdStrategy.DROP_ID_TABLES, "true" );
		if ( !Environment.getProperties().containsKey( Environment.CONNECTION_PROVIDER ) ) {
			settings.put(
					AvailableSettings.CONNECTION_PROVIDER,
					SharedDriverManagerConnectionProviderImpl.getInstance()
			);
		}
		if ( defaultCatalog != null ) {
			settings.put( AvailableSettings.DEFAULT_CATALOG, defaultCatalog );
		}
		if ( defaultSchema != null ) {
			settings.put( AvailableSettings.DEFAULT_SCHEMA, defaultSchema );
		}

		final StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder( bsr );
		ssrb.applySettings( settings );
		StandardServiceRegistry registry = ssrb.build();
		toClose.add( registry );
		return registry;
	}

	@Test
	public void createSchema_fromSessionFactory() {
		String script = generateScriptFromSessionFactory( "create" );
		verifyDDLQualifiers( script );
	}

	@Test
	public void dropSchema_fromSessionFactory() {
		String script = generateScriptFromSessionFactory( "drop" );
		verifyDDLQualifiers( script );
	}

	@Test
	public void createSchema_fromMetadata() {
		String script = generateScriptFromMetadata( SchemaExport.Action.CREATE );
		verifyDDLQualifiers( script );
	}

	@Test
	public void dropSchema_fromMetadata() {
		String script = generateScriptFromMetadata( SchemaExport.Action.DROP );
		verifyDDLQualifiers( script );
	}

	@Test
	public void entityPersister() {
		verifyEntityPersisterQualifiers( EntityWithDefaultQualifiers.class, expectedDefaultQualifier() );
		verifyEntityPersisterQualifiers( EntityWithExplicitQualifiers.class, expectedExplicitQualifier() );
		verifyEntityPersisterQualifiers( EntityWithOrmXmlImplicitFileLevelQualifiers.class, expectedImplicitFileLevelQualifier() );
		verifyEntityPersisterQualifiers( EntityWithHbmXmlImplicitFileLevelQualifiers.class, expectedImplicitFileLevelQualifier() );

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
		verifyEntityPersisterQualifiers( EntityWithDefaultQualifiersWithSeqHiLoGenerator.class, expectedDefaultQualifier() );
		verifyEntityPersisterQualifiers( EntityWithExplicitQualifiersWithSeqHiLoGenerator.class, expectedExplicitQualifier() );
		verifyEntityPersisterQualifiers( EntityWithDefaultQualifiersWithIncrementGenerator.class, expectedDefaultQualifier() );
		verifyEntityPersisterQualifiers( EntityWithExplicitQualifiersWithIncrementGenerator.class, expectedExplicitQualifier() );
		verifyEntityPersisterQualifiers( EntityWithDefaultQualifiersWithSequenceIdentityGenerator.class, expectedDefaultQualifier() );
		verifyEntityPersisterQualifiers( EntityWithExplicitQualifiersWithSequenceIdentityGenerator.class, expectedExplicitQualifier() );
		verifyEntityPersisterQualifiers( EntityWithDefaultQualifiersWithEnhancedSequenceGenerator.class, expectedDefaultQualifier() );
		verifyEntityPersisterQualifiers( EntityWithExplicitQualifiersWithEnhancedSequenceGenerator.class, expectedExplicitQualifier() );
		verifyEntityPersisterQualifiers( EntityWithDefaultQualifiersWithLegacySequenceGenerator.class, expectedDefaultQualifier() );
		verifyEntityPersisterQualifiers( EntityWithExplicitQualifiersWithLegacySequenceGenerator.class, expectedExplicitQualifier() );
	}

	private void verifyEntityPersisterQualifiers(Class<?> entityClass, String expectedQualifier) {
		String jpaEntityName = sessionFactory.getMetamodel().entity( entityClass ).getName();
		Map<String, EntityPersister> persisters = sessionFactory.getMetamodel().entityPersisters();
		AbstractEntityPersister persister = (AbstractEntityPersister) persisters.get( entityClass.getName() );
		if ( persister == null ) {
			// hbm.xml mapping sets the native entity name on top of the JPA entity name,
			// so we must use a different key here.
			persister = (AbstractEntityPersister) persisters.get( jpaEntityName );
		}
		if ( persister == null ) {
			throw new IllegalStateException( "Cannot find persister for " + entityClass );
		}

		// Table names are what's used for Query, in particular.
		verifyOnlyQualifier( persister.getTableName(), jpaEntityName, expectedQualifier );
		// Here, to simplify assertions, we assume all derived entity types have:
		// - an entity name prefixed with the name of their super entity type
		// - the same explicit catalog and schema, if any, as their super entity type
		verifyOnlyQualifier( persister.getTableNames(), jpaEntityName, expectedQualifier );

		// This will include SQL generated by ID generators in some cases, which will be validated here
		// because ID generators table/sequence names are prefixed with the owning entity name.
		verifyOnlyQualifier( persister.getSQLInsertStrings(), jpaEntityName, expectedQualifier );
		if ( persister.isIdentifierAssignedByInsert() ) {
			verifyOnlyQualifier( persister.getSQLIdentityInsertString(), jpaEntityName, expectedQualifier );
		}
		verifyOnlyQualifierOptional( persister.getIdentitySelectString(), jpaEntityName, expectedQualifier );

		verifyOnlyQualifier( persister.getSQLUpdateStrings(), jpaEntityName, expectedQualifier );
		verifyOnlyQualifier( persister.getSQLLazyUpdateStrings(), jpaEntityName, expectedQualifier );

		verifyOnlyQualifier( persister.getSQLDeleteStrings(), jpaEntityName, expectedQualifier );

		verifyOnlyQualifier( persister.getSQLSnapshotSelectString(), jpaEntityName, expectedQualifier );

		// This is used in the "select" id generator in particular.
		verifyOnlyQualifierOptional( persister.getSelectByUniqueKeyString( "basic" ), jpaEntityName, expectedQualifier );
	}

	@Test
	public void tableGenerator() {
		org.hibernate.id.enhanced.TableGenerator generator = idGenerator(
				org.hibernate.id.enhanced.TableGenerator.class,
						EntityWithDefaultQualifiersWithTableGenerator.class );
		verifyOnlyQualifier( generator.getAllSqlForTests(),
				EntityWithDefaultQualifiersWithTableGenerator.NAME, expectedDefaultQualifier() );

		generator = idGenerator( org.hibernate.id.enhanced.TableGenerator.class,
				EntityWithExplicitQualifiersWithTableGenerator.class );
		verifyOnlyQualifier( generator.getAllSqlForTests(),
				EntityWithExplicitQualifiersWithTableGenerator.NAME, expectedExplicitQualifier() );
	}

	@Test
	public void enhancedTableGenerator() {
		org.hibernate.id.enhanced.TableGenerator generator = idGenerator(
				org.hibernate.id.enhanced.TableGenerator.class,
				EntityWithDefaultQualifiersWithTableGenerator.class );
		verifyOnlyQualifier( generator.getAllSqlForTests(),
				EntityWithDefaultQualifiersWithTableGenerator.NAME, expectedDefaultQualifier() );

		generator = idGenerator( org.hibernate.id.enhanced.TableGenerator.class,
				EntityWithExplicitQualifiersWithTableGenerator.class );
		verifyOnlyQualifier( generator.getAllSqlForTests(),
				EntityWithExplicitQualifiersWithTableGenerator.NAME, expectedExplicitQualifier() );
	}

	@Test
	public void sequenceGenerator() {
		org.hibernate.id.enhanced.SequenceStyleGenerator generator = idGenerator(
				org.hibernate.id.enhanced.SequenceStyleGenerator.class,
				EntityWithDefaultQualifiersWithSequenceGenerator.class );
		verifyOnlyQualifier( generator.getDatabaseStructure().getAllSqlForTests(),
				EntityWithDefaultQualifiersWithSequenceGenerator.NAME, expectedDefaultQualifier() );

		generator = idGenerator( org.hibernate.id.enhanced.SequenceStyleGenerator.class,
				EntityWithExplicitQualifiersWithSequenceGenerator.class );
		verifyOnlyQualifier( generator.getDatabaseStructure().getAllSqlForTests(),
				EntityWithExplicitQualifiersWithSequenceGenerator.NAME, expectedExplicitQualifier() );
	}

	@Test
	public void enhancedSequenceGenerator() {
		org.hibernate.id.enhanced.SequenceStyleGenerator generator = idGenerator(
				org.hibernate.id.enhanced.SequenceStyleGenerator.class,
				EntityWithDefaultQualifiersWithEnhancedSequenceGenerator.class );
		verifyOnlyQualifier( generator.getDatabaseStructure().getAllSqlForTests(),
				EntityWithDefaultQualifiersWithEnhancedSequenceGenerator.NAME, expectedDefaultQualifier() );

		generator = idGenerator( org.hibernate.id.enhanced.SequenceStyleGenerator.class,
				EntityWithExplicitQualifiersWithEnhancedSequenceGenerator.class );
		verifyOnlyQualifier( generator.getDatabaseStructure().getAllSqlForTests(),
				EntityWithExplicitQualifiersWithEnhancedSequenceGenerator.NAME, expectedExplicitQualifier() );
	}

	@Test
	public void legacySequenceGenerator() {
		org.hibernate.id.SequenceGenerator generator = idGenerator( org.hibernate.id.SequenceGenerator.class,
				EntityWithDefaultQualifiersWithLegacySequenceGenerator.class );
		verifyOnlyQualifier( generator.getAllSqlForTests(),
				EntityWithDefaultQualifiersWithLegacySequenceGenerator.NAME, expectedDefaultQualifier() );

		generator = idGenerator( org.hibernate.id.SequenceGenerator.class,
				EntityWithExplicitQualifiersWithLegacySequenceGenerator.class );
		verifyOnlyQualifier( generator.getAllSqlForTests(),
				EntityWithExplicitQualifiersWithLegacySequenceGenerator.NAME, expectedExplicitQualifier() );
	}

	@Test
	public void seqHiLoGenerator() {
		org.hibernate.id.SequenceHiLoGenerator generator = idGenerator( org.hibernate.id.SequenceHiLoGenerator.class,
				EntityWithDefaultQualifiersWithSeqHiLoGenerator.class );
		verifyOnlyQualifier( generator.getAllSqlForTests(),
				EntityWithDefaultQualifiersWithSeqHiLoGenerator.NAME, expectedDefaultQualifier() );

		generator = idGenerator( org.hibernate.id.SequenceHiLoGenerator.class,
				EntityWithExplicitQualifiersWithSeqHiLoGenerator.class );
		verifyOnlyQualifier( generator.getAllSqlForTests(),
				EntityWithExplicitQualifiersWithSeqHiLoGenerator.NAME, expectedExplicitQualifier() );
	}

	@Test
	public void incrementGenerator() {
		org.hibernate.id.IncrementGenerator generator = idGenerator( org.hibernate.id.IncrementGenerator.class,
				EntityWithDefaultQualifiersWithIncrementGenerator.class );
		verifyOnlyQualifier( generator.getAllSqlForTests(),
				EntityWithDefaultQualifiersWithIncrementGenerator.NAME, expectedDefaultQualifier() );

		generator = idGenerator( org.hibernate.id.IncrementGenerator.class,
				EntityWithExplicitQualifiersWithIncrementGenerator.class );
		verifyOnlyQualifier( generator.getAllSqlForTests(),
				EntityWithExplicitQualifiersWithIncrementGenerator.NAME, expectedExplicitQualifier() );
	}

	@Test
	public void sequenceIdentityGenerator() {
		org.hibernate.id.SequenceIdentityGenerator generator = idGenerator( org.hibernate.id.SequenceIdentityGenerator.class,
				EntityWithDefaultQualifiersWithSequenceIdentityGenerator.class );
		verifyOnlyQualifier( generator.getAllSqlForTests(),
				EntityWithDefaultQualifiersWithSequenceIdentityGenerator.NAME, expectedDefaultQualifier() );

		generator = idGenerator( org.hibernate.id.SequenceIdentityGenerator.class,
				EntityWithExplicitQualifiersWithSequenceIdentityGenerator.class );
		verifyOnlyQualifier( generator.getAllSqlForTests(),
				EntityWithExplicitQualifiersWithSequenceIdentityGenerator.NAME, expectedExplicitQualifier() );
	}

	private <T extends IdentifierGenerator> T idGenerator(Class<T> expectedType, Class<?> entityClass) {
		AbstractEntityPersister persister = (AbstractEntityPersister)
				sessionFactory.getMetamodel().entityPersister( entityClass );
		return expectedType.cast( persister.getIdentifierGenerator() );
	}

	private void verifyDDLQualifiers(String sql) {
		// Here, to simplify assertions, we assume:
		// - that all entity types have a table name identical to the entity name
		// - that all association tables have a name prefixed with the name of their owning entity type
		// - that all association tables have the same explicit catalog and schema, if any, as their owning entity type
		// - that all ID generator tables/sequences have a name prefixed with the name of their owning entity type

		verifyOnlyQualifier( sql, EntityWithExplicitQualifiers.NAME, expectedExplicitQualifier() );
		verifyOnlyQualifier( sql, EntityWithDefaultQualifiers.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, EntityWithOrmXmlImplicitFileLevelQualifiers.NAME, expectedImplicitFileLevelQualifier() );
		verifyOnlyQualifier( sql, EntityWithHbmXmlImplicitFileLevelQualifiers.NAME, expectedImplicitFileLevelQualifier() );

		verifyOnlyQualifier( sql, EntityWithJoinedInheritanceWithDefaultQualifiers.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, EntityWithJoinedInheritanceWithDefaultQualifiersSubclass.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, EntityWithJoinedInheritanceWithExplicitQualifiers.NAME, expectedExplicitQualifier() );
		verifyOnlyQualifier( sql, EntityWithJoinedInheritanceWithExplicitQualifiersSubclass.NAME, expectedExplicitQualifier() );

		verifyOnlyQualifier( sql, EntityWithTablePerClassInheritanceWithDefaultQualifiers.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, EntityWithTablePerClassInheritanceWithDefaultQualifiersSubclass.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, EntityWithTablePerClassInheritanceWithExplicitQualifiers.NAME, expectedExplicitQualifier() );
		verifyOnlyQualifier( sql, EntityWithTablePerClassInheritanceWithExplicitQualifiersSubclass.NAME, expectedExplicitQualifier() );

		verifyOnlyQualifier( sql, EntityWithDefaultQualifiersWithCustomSql.NAME, expectedDefaultQualifier() );

		verifyOnlyQualifier( sql, EntityWithDefaultQualifiersWithIdentityGenerator.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, EntityWithExplicitQualifiersWithIdentityGenerator.NAME, expectedExplicitQualifier() );
		verifyOnlyQualifier( sql, EntityWithDefaultQualifiersWithTableGenerator.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, EntityWithExplicitQualifiersWithTableGenerator.NAME, expectedExplicitQualifier() );
		verifyOnlyQualifier( sql, EntityWithDefaultQualifiersWithSequenceGenerator.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, EntityWithExplicitQualifiersWithSequenceGenerator.NAME, expectedExplicitQualifier() );
		verifyOnlyQualifier( sql, EntityWithDefaultQualifiersWithSeqHiLoGenerator.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, EntityWithExplicitQualifiersWithSeqHiLoGenerator.NAME, expectedExplicitQualifier() );
		verifyOnlyQualifier( sql, EntityWithDefaultQualifiersWithIncrementGenerator.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, EntityWithExplicitQualifiersWithIncrementGenerator.NAME, expectedExplicitQualifier() );
		verifyOnlyQualifier( sql, EntityWithDefaultQualifiersWithSequenceIdentityGenerator.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, EntityWithExplicitQualifiersWithSequenceIdentityGenerator.NAME, expectedExplicitQualifier() );
		verifyOnlyQualifier( sql, EntityWithDefaultQualifiersWithEnhancedSequenceGenerator.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, EntityWithExplicitQualifiersWithEnhancedSequenceGenerator.NAME, expectedExplicitQualifier() );
		verifyOnlyQualifier( sql, EntityWithDefaultQualifiersWithLegacySequenceGenerator.NAME, expectedDefaultQualifier() );
		verifyOnlyQualifier( sql, EntityWithExplicitQualifiersWithLegacySequenceGenerator.NAME, expectedExplicitQualifier() );
	}

	private void verifyOnlyQualifier(String[] sql, String name, String expectedQualifier) {
		verifyOnlyQualifier( String.join( "\n", sql ), name, expectedQualifier );
	}

	private void verifyOnlyQualifierOptional(String sql, String name, String expectedQualifier) {
		verifyOnlyQualifier( sql, name, expectedQualifier, true );
	}

	private void verifyOnlyQualifier(String sql, String name, String expectedQualifier) {
		verifyOnlyQualifier( sql, name, expectedQualifier, false );
	}

	private void verifyOnlyQualifier(String sql, String name, String expectedQualifier, boolean optional) {
		String namePattern = SQL_QUOTE_CHARACTER_CLASS + "?" + Pattern.quote( name ) + SQL_QUOTE_CHARACTER_CLASS + "?";
		Pattern wantedPattern;
		Pattern unwantedPattern;
		if ( expectedQualifier.isEmpty() ) {
			// We can find the unqualified name
			wantedPattern = Pattern.compile( "(?<!\\.)" + namePattern );
			// We cannot find the qualified name
			unwantedPattern = Pattern.compile( "\\." + namePattern );
		}
		else {
			String qualifierPattern = Pattern.quote( expectedQualifier );
			// We can find the qualified name
			wantedPattern = Pattern.compile( qualifierPattern + namePattern );
			// We cannot find the name with a different qualifier, except in strings (preceded with a single-quote)
			unwantedPattern = Pattern.compile( "(?<!" + qualifierPattern + "|')" + namePattern );
		}

		if ( !optional ) {
			assertThat( sql )
					.containsPattern( wantedPattern );
		}
		// Check the unwanted pattern line by line, so that the error message mentions the exact line
		assertThat( sql.split( System.getProperty( "line.separator" ) ) )
				.allSatisfy( line -> assertThat( line )
						.doesNotContainPattern( unwantedPattern ) );
	}

	private String expectedDefaultQualifier() {
		return ( expectedDefaultCatalog != null ? expectedDefaultCatalog + "." : "" )
				+ ( expectedDefaultSchema != null ? expectedDefaultSchema + "." : "" );
	}

	private String expectedExplicitQualifier() {
		return EXPLICIT_CATALOG + "." + EXPLICIT_SCHEMA + ".";
	}

	private String expectedImplicitFileLevelQualifier() {
		return "someImplicitFileLevelCatalog.someImplicitFileLevelSchema.";
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private String generateScriptFromSessionFactory(String action) {
		ServiceRegistryImplementor serviceRegistry = sessionFactory.getServiceRegistry();
		Map<String, Object> settings = new HashMap<>(
				serviceRegistry.getService( ConfigurationService.class ).getSettings()
		);
		StringWriter writer = new StringWriter();
		settings.put( AvailableSettings.HBM2DDL_SCRIPTS_ACTION, action );
		settings.put( AvailableSettings.HBM2DDL_SCRIPTS_CREATE_TARGET, writer );
		settings.put( AvailableSettings.HBM2DDL_SCRIPTS_DROP_TARGET, writer );

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
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = NAME + "_generator")
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
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = NAME + "_generator")
		@TableGenerator(name = NAME + "_generator", table = NAME + "_tableseq",
				catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA)
		private Long id;
		@Basic
		private String basic;
	}

	@Entity(name = EntityWithDefaultQualifiersWithSeqHiLoGenerator.NAME)
	public static class EntityWithDefaultQualifiersWithSeqHiLoGenerator {
		public static final String NAME = "EntityWithDefaultQualifiersWithSeqHiLoGenerator";
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = NAME + "_generator")
		@GenericGenerator(name = NAME + "_generator", strategy = "org.hibernate.id.SequenceHiLoGenerator", parameters = {
				@Parameter(name = "sequence", value = NAME + "_seq"),
				@Parameter(name = "max_lo", value = "5")
		})
		private Long id;
		@Basic
		private String basic;
	}

	@Entity(name = EntityWithExplicitQualifiersWithSeqHiLoGenerator.NAME)
	@Table(catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA)
	public static class EntityWithExplicitQualifiersWithSeqHiLoGenerator {
		public static final String NAME = "EntityWithExplicitQualifiersWithSeqHiLoGenerator";
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = NAME + "_generator")
		@GenericGenerator(name = NAME + "_generator", strategy = "org.hibernate.id.SequenceHiLoGenerator", parameters = {
				@Parameter(name = "sequence", value = NAME + "_seq"),
				@Parameter(name = "max_lo", value = "5"),
				@Parameter(name = "catalog", value = EXPLICIT_CATALOG),
				@Parameter(name = "schema", value = EXPLICIT_SCHEMA)
		})
		private Long id;
		@Basic
		private String basic;
	}

	@Entity(name = EntityWithDefaultQualifiersWithIncrementGenerator.NAME)
	public static class EntityWithDefaultQualifiersWithIncrementGenerator {
		public static final String NAME = "EntityWithDefaultQualifiersWithIncrementGenerator";
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = NAME + "_generator")
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
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = NAME + "_generator")
		@GenericGenerator(name = NAME + "_generator", strategy = "increment", parameters = {
				@Parameter(name = "catalog", value = EXPLICIT_CATALOG),
				@Parameter(name = "schema", value = EXPLICIT_SCHEMA)
		})
		private Long id;
		@Basic
		private String basic;
	}

	@Entity(name = EntityWithDefaultQualifiersWithSequenceIdentityGenerator.NAME)
	public static class EntityWithDefaultQualifiersWithSequenceIdentityGenerator {
		public static final String NAME = "EntityWithDefaultQualifiersWithSequenceIdentityGenerator";
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = NAME + "_generator")
		@GenericGenerator(name = NAME + "_generator", strategy = "sequence-identity", parameters = {
				@Parameter(name = "sequence", value = NAME + "_seq")
		})
		private Long id;
		@Basic
		private String basic;
	}

	@Entity(name = EntityWithExplicitQualifiersWithSequenceIdentityGenerator.NAME)
	@Table(catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA)
	public static class EntityWithExplicitQualifiersWithSequenceIdentityGenerator {
		public static final String NAME = "EntityWithExplicitQualifiersWithSequenceIdentityGenerator";
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = NAME + "_generator")
		@GenericGenerator(name = NAME + "_generator", strategy = "sequence-identity", parameters = {
				@Parameter(name = "sequence", value = NAME + "_seq"),
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
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = NAME + "_generator")
		@GenericGenerator(name = NAME + "_generator", strategy = "enhanced-sequence", parameters = {
				@Parameter(name = "sequence", value = NAME + "_seq")
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
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = NAME + "_generator")
		@GenericGenerator(name = NAME + "_generator", strategy = "enhanced-sequence", parameters = {
				@Parameter(name = "sequence", value = NAME + "_seq"),
				@Parameter(name = "catalog", value = EXPLICIT_CATALOG),
				@Parameter(name = "schema", value = EXPLICIT_SCHEMA)
		})
		private Long id;
		@Basic
		private String basic;
	}


	@Entity(name = EntityWithDefaultQualifiersWithLegacySequenceGenerator.NAME)
	public static class EntityWithDefaultQualifiersWithLegacySequenceGenerator {
		public static final String NAME = "EntityWithDefaultQualifiersWithLegacySequenceGenerator";
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = NAME + "_generator")
		@GenericGenerator(name = NAME + "_generator", strategy = "org.hibernate.id.SequenceGenerator", parameters = {
				@Parameter(name = "sequence", value = NAME + "_seq")
		})
		private Long id;
		@Basic
		private String basic;
	}

	@Entity(name = EntityWithExplicitQualifiersWithLegacySequenceGenerator.NAME)
	@Table(catalog = EXPLICIT_CATALOG, schema = EXPLICIT_SCHEMA)
	public static class EntityWithExplicitQualifiersWithLegacySequenceGenerator {
		public static final String NAME = "EntityWithExplicitQualifiersWithLegacySequenceGenerator";
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = NAME + "_generator")
		@GenericGenerator(name = NAME + "_generator", strategy = "org.hibernate.id.SequenceGenerator", parameters = {
				@Parameter(name = "sequence", value = NAME + "_seq"),
				@Parameter(name = "catalog", value = EXPLICIT_CATALOG),
				@Parameter(name = "schema", value = EXPLICIT_SCHEMA)
		})
		private Long id;
		@Basic
		private String basic;
	}

}
