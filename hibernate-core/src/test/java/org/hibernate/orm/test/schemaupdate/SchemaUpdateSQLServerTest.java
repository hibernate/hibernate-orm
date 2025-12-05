/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import org.hamcrest.MatcherAssert;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.transaction.TransactionUtil;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.JdbcMetadataAccessStrategy;
import org.hibernate.tool.schema.TargetType;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.nio.file.Files;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.hamcrest.core.Is.is;
import static org.hibernate.cfg.MappingSettings.KEYWORD_AUTO_QUOTING_ENABLED;
import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(SQLServerDialect.class)
@ParameterizedClass
@MethodSource("parameters")
@TestInstance( TestInstance.Lifecycle.PER_METHOD )
@ServiceRegistryFunctionalTesting
@DomainModel(annotatedClasses = {
		SchemaUpdateSQLServerTest.LowercaseTableNameEntity.class,
		SchemaUpdateSQLServerTest.TestEntity.class,
		SchemaUpdateSQLServerTest.UppercaseTableNameEntity.class,
		SchemaUpdateSQLServerTest.MixedCaseTableNameEntity.class,
		SchemaUpdateSQLServerTest.Match.class,
		SchemaUpdateSQLServerTest.InheritanceRootEntity.class,
		SchemaUpdateSQLServerTest.InheritanceChildEntity.class,
		SchemaUpdateSQLServerTest.InheritanceSecondChildEntity.class
})
public class SchemaUpdateSQLServerTest implements ServiceRegistryProducer {
	private static final Logger log =  Logger.getLogger( SchemaUpdateSQLServerTest.class );

	public static Collection<JdbcMetadataAccessStrategy> parameters() {
		return List.of(
				JdbcMetadataAccessStrategy.GROUPED,
				JdbcMetadataAccessStrategy.INDIVIDUALLY
		);
	}

	public JdbcMetadataAccessStrategy jdbcMetadataExtractorStrategy;
	private final File output;

	public SchemaUpdateSQLServerTest(
			JdbcMetadataAccessStrategy jdbcMetadataExtractorStrategy,
			@TempDir File outputDir) {
		this.jdbcMetadataExtractorStrategy = jdbcMetadataExtractorStrategy;
		this.output = new File( outputDir, "update_script.sql" );
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		return ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( KEYWORD_AUTO_QUOTING_ENABLED, "true" )
				.applySetting( HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY, jdbcMetadataExtractorStrategy )
				.build();
	}

	@BeforeEach
	void prepare(ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		var registry = registryScope.getRegistry();

		try {
			TransactionUtil.doWithJDBC( registry, connection -> {
				try (Statement statement = connection.createStatement()) {
					connection.setAutoCommit( true );
					statement.executeUpdate( "DROP DATABASE hibernate_orm_test_collation" );
				}
			} );
		}
		catch (SQLException e) {
			log.debug( e.getMessage() );
		}
		try {
			TransactionUtil.doWithJDBC( registry, connection -> {
				try (Statement statement = connection.createStatement()) {
					connection.setAutoCommit( true );
					statement.executeUpdate( "CREATE DATABASE hibernate_orm_test_collation COLLATE Latin1_General_CS_AS" );
					statement.executeUpdate( "ALTER DATABASE [hibernate_orm_test_collation] SET AUTO_CLOSE OFF " );
				}
			} );
		}
		catch (SQLException e) {
			log.debug( e.getMessage() );
		}

		var model = modelScope.getDomainModel();
		model.orderColumns( false );
		model.validate();
	}

	@AfterEach
	void tearsDown(DomainModelScope modelScope) {
		new SchemaExport().setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.drop( EnumSet.of( TargetType.DATABASE ), modelScope.getDomainModel() );
	}

	@Test
	public void testSchemaUpdateAndValidation(DomainModelScope modelScope) throws Exception {
		new SchemaUpdate()
				.setHaltOnError( true )
				.execute( EnumSet.of( TargetType.DATABASE ), modelScope.getDomainModel() );

		new SchemaValidator().validate( modelScope.getDomainModel() );

		new SchemaUpdate()
				.setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), modelScope.getDomainModel() );

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		MatcherAssert.assertThat( "The update output file should be empty", fileContent, is( "" ) );
	}

	@Entity(name = "TestEntity")
	@Table(name = "`testentity`", catalog = "hibernate_orm_test_collation", schema = "dbo")
	public static class LowercaseTableNameEntity {
		@Id
		long id;
		String field1;

		@ManyToMany(mappedBy = "entities")
		Set<TestEntity> entity1s;
	}

	@Entity(name = "TestEntity1")
	@Table(name = "TestEntity1", catalog = "hibernate_orm_test_collation", schema = "dbo")
	public static class TestEntity {
		@Id
		@Column(name = "`Id`")
		long id;
		String field1;

		@ManyToMany
		@JoinTable(catalog = "hibernate_orm_test_collation", schema = "dbo")
		Set<LowercaseTableNameEntity> entities;

		@OneToMany
		@JoinColumn
		private Set<UppercaseTableNameEntity> entitie2s;

		@ManyToOne
		private LowercaseTableNameEntity entity;
	}

	@Entity(name = "TestEntity2")
	@Table(name = "`TESTENTITY`", catalog = "hibernate_orm_test_collation", schema = "dbo")
	public static class UppercaseTableNameEntity {
		@Id
		long id;
		String field1;

		@ManyToOne
		TestEntity testEntity;

		@ManyToOne
		@JoinColumn(foreignKey = @ForeignKey(name = "FK_mixedCase"))
		MixedCaseTableNameEntity mixedCaseTableNameEntity;
	}

	@Entity(name = "TestEntity3")
	@Table(name = "`TESTentity`", catalog = "hibernate_orm_test_collation", schema = "dbo",
			indexes = {@Index(name = "index1", columnList = "`FieLd1`"), @Index(name = "Index2", columnList = "`FIELD_2`")})
	public static class MixedCaseTableNameEntity {
		@Id
		long id;
		@Column(name = "`FieLd1`")
		String field1;
		@Column(name = "`FIELD_2`")
		String field2;
		@Column(name = "`field_3`")
		String field3;
		String field4;

		@OneToMany
		@JoinColumn
		private Set<Match> matches = new HashSet<>();
	}

	@Entity(name = "Match")
	@Table(name = "Match", catalog = "hibernate_orm_test_collation", schema = "dbo")
	public static class Match {
		@Id
		long id;
		String match;

		@ElementCollection
		@CollectionTable(catalog = "hibernate_orm_test_collation", schema = "dbo")
		private Map<Integer, Integer> timeline = new TreeMap<>();
	}

	@Entity(name = "InheritanceRootEntity")
	@Table(name = "InheritanceRootEntity", catalog = "hibernate_orm_test_collation", schema = "dbo")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class InheritanceRootEntity {
		@Id
		protected Long id;
	}

	@Entity(name = "InheritanceChildEntity")
	@Table(name = "InheritanceChildEntity", catalog = "hibernate_orm_test_collation", schema = "dbo")
	@PrimaryKeyJoinColumn(name = "ID", foreignKey = @ForeignKey(name = "FK_ROOT"))
	public static class InheritanceChildEntity extends InheritanceRootEntity {
	}

	@Entity(name = "InheritanceSecondChildEntity")
	@Table(name = "InheritanceSecondChildEntity", catalog = "hibernate_orm_test_collation", schema = "dbo")
	@PrimaryKeyJoinColumn(name = "ID")
	public static class InheritanceSecondChildEntity extends InheritanceRootEntity {
		@ManyToOne
		@JoinColumn
		public Match match;
	}
}
