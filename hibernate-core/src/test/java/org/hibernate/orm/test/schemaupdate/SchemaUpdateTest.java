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
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import org.hamcrest.MatcherAssert;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.testing.SkipLog;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistryFunctionalTesting;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.JdbcMetadataAccessStrategy;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.nio.file.Files;
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
@SkipForDialect(dialectClass = SQLServerDialect.class)
@SkipForDialect(dialectClass = SybaseDialect.class)
@SkipForDialect(dialectClass = TiDBDialect.class)
@ParameterizedClass
@MethodSource("parameters")
@TestInstance( TestInstance.Lifecycle.PER_METHOD )
@ServiceRegistryFunctionalTesting
@DomainModel(annotatedClasses = {
		SchemaUpdateTest.LowercaseTableNameEntity.class,
		SchemaUpdateTest.TestEntity.class,
		SchemaUpdateTest.UppercaseTableNameEntity.class,
		SchemaUpdateTest.MixedCaseTableNameEntity.class,
		SchemaUpdateTest.Match.class,
		SchemaUpdateTest.InheritanceRootEntity.class,
		SchemaUpdateTest.InheritanceChildEntity.class,
		SchemaUpdateTest.InheritanceSecondChildEntity.class
})
public class SchemaUpdateTest implements ServiceRegistryProducer {
	public static Collection<JdbcMetadataAccessStrategy> parameters() {
		return List.of(
				JdbcMetadataAccessStrategy.GROUPED,
				JdbcMetadataAccessStrategy.INDIVIDUALLY
		);
	}

	public JdbcMetadataAccessStrategy jdbcMetadataExtractorStrategy;
	private final File output;

	private boolean skip;

	public SchemaUpdateTest(
			JdbcMetadataAccessStrategy jdbcMetadataExtractorStrategy,
			@TempDir File outputDir) {
		this.jdbcMetadataExtractorStrategy = jdbcMetadataExtractorStrategy;
		this.output = new File( outputDir, "update_script.sql" );
	}

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		return builder.applySetting( KEYWORD_AUTO_QUOTING_ENABLED, true )
				.applySetting( HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY, jdbcMetadataExtractorStrategy )
				.build();
	}

	@BeforeEach
	void prepare(ServiceRegistryScope registryScope) {
		// Databases that use case-insensitive quoted identifiers need to be skipped.
		// The following checks will work for checking those dialects that store case-insensitive
		// quoted identifiers as upper-case or lower-case. It does not work for dialects that
		// store case-insensitive identifiers in mixed case (like SQL Server).
		final IdentifierHelper identifierHelper  = registryScope.getRegistry().requireService( JdbcEnvironment.class ).getIdentifierHelper();
		final String lowerCaseName = identifierHelper.toMetaDataObjectName( Identifier.toIdentifier( "testentity", true ) );
		final String upperCaseName = identifierHelper.toMetaDataObjectName( Identifier.toIdentifier("TESTENTITY", true ) );
		final String mixedCaseName = identifierHelper.toMetaDataObjectName( Identifier.toIdentifier("TESTentity", true ) );
		if ( lowerCaseName.equals( upperCaseName ) ||
			lowerCaseName.equals( mixedCaseName ) ||
			upperCaseName.equals( mixedCaseName ) ) {
			skip = true;
		}
	}

	@AfterEach
	public void tearsDown(DomainModelScope modelScope) {
		if ( skip ) {
			return;
		}

		new SchemaExport().setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.drop( EnumSet.of( TargetType.DATABASE ), modelScope.getDomainModel() );
	}

	@Test
	public void testSchemaUpdateAndValidation(DomainModelScope modelScope) throws Exception {
		if ( skip ) {
			SkipLog.reportSkip( "skipping test because quoted names are not case-sensitive." );
			return;
		}

		new SchemaUpdate().setHaltOnError( true )
				.execute( EnumSet.of( TargetType.DATABASE ), modelScope.getDomainModel() );

		new SchemaValidator().validate( modelScope.getDomainModel() );

		new SchemaUpdate().setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), modelScope.getDomainModel() );

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		MatcherAssert.assertThat( "The update output file should be empty", fileContent, is( "" ) );
	}

	@Entity(name = "TestEntity")
	@Table(name = "`testentity`")
	public static class LowercaseTableNameEntity {
		@Id
		long id;
		String field1;

		@ManyToMany(mappedBy = "entities")
		Set<TestEntity> entity1s;
	}

	@Entity(name = "TestEntity1")
	public static class TestEntity {
		@Id
		@Column(name = "`Id`")
		long id;
		String field1;

		@ManyToMany
		Set<LowercaseTableNameEntity> entities;

		@OneToMany
		@JoinColumn
		private Set<UppercaseTableNameEntity> entitie2s;

		@ManyToOne
		private LowercaseTableNameEntity entity;
	}

	@Entity(name = "TestEntity2")
	@Table(name = "`TESTENTITY`")
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

	@SuppressWarnings("unused")
	@Entity(name = "TestEntity3")
	@Table(name = "`TESTentity`", indexes = {@Index(name = "index1", columnList = "`FieLd1`"), @Index(name = "Index2", columnList = "`FIELD_2`")})
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

	@SuppressWarnings("unused")
	@Entity(name = "Match")
	public static class Match {
		@Id
		long id;
		String match;

		@ElementCollection
		@CollectionTable
		private Map<Integer, Integer> timeline = new TreeMap<>();
	}

	@Entity(name = "InheritanceRootEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class InheritanceRootEntity {
		@Id
		protected Long id;
	}

	@Entity(name = "InheritanceChildEntity")
	@PrimaryKeyJoinColumn(name = "ID", foreignKey = @ForeignKey(name = "FK_ROOT"))
	public static class InheritanceChildEntity extends InheritanceRootEntity {
	}

	@Entity(name = "InheritanceSecondChildEntity")
	@PrimaryKeyJoinColumn(name = "ID")
	public static class InheritanceSecondChildEntity extends InheritanceRootEntity {
		@ManyToOne
		@JoinColumn
		public Match match;
	}
}
