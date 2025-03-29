/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.TiDBDialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.JdbcMetadaAccessStrategy;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.SkipLog;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@RunWith(Parameterized.class)
public class SchemaUpdateTest {

	private boolean skipTest;

	@Parameterized.Parameters
	public static Collection<String> parameters() {
		return Arrays.asList(
				new String[] {JdbcMetadaAccessStrategy.GROUPED.toString(), JdbcMetadaAccessStrategy.INDIVIDUALLY.toString()}
		);
	}

	@Parameterized.Parameter
	public String jdbcMetadataExtractorStrategy;

	private File output;
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	@Before
	public void setUp() throws IOException {
		if ( SQLServerDialect.class.isAssignableFrom( DialectContext.getDialect().getClass() )
				|| SybaseDialect.class.isAssignableFrom( DialectContext.getDialect().getClass() )
				|| TiDBDialect.class.isAssignableFrom( DialectContext.getDialect().getClass() ) ) {
			// SQLServerDialect, SybaseDialect and TiDB store case-insensitive quoted identifiers in mixed case,
			// so the checks at the end of this method won't work.
			// For TiDB, only 'lower_case_table_names=2' is supported.
			skipTest = true;
			return;
		}
		output = File.createTempFile( "update_script", ".sql" );
		output.deleteOnExit();
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED, "true" )
				.applySetting( AvailableSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY, jdbcMetadataExtractorStrategy )
				.build();

		final MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( LowercaseTableNameEntity.class );
		metadataSources.addAnnotatedClass( TestEntity.class );
		metadataSources.addAnnotatedClass( UppercaseTableNameEntity.class );
		metadataSources.addAnnotatedClass( MixedCaseTableNameEntity.class );
		metadataSources.addAnnotatedClass( Match.class );
		metadataSources.addAnnotatedClass( InheritanceRootEntity.class );
		metadataSources.addAnnotatedClass( InheritanceChildEntity.class );
		metadataSources.addAnnotatedClass( InheritanceSecondChildEntity.class );

		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();

		// Databases that use case-insensitive quoted identifiers need to be skipped.
		// The following checks will work for checking those dialects that store case-insensitive
		// quoted identifiers as upper-case or lower-case. It does not work for dialects that
		// store case-insensitive identifiers in mixed case (like SQL Server).
		final IdentifierHelper identifierHelper  = ssr.getService( JdbcEnvironment.class ).getIdentifierHelper();
		final String lowerCaseName = identifierHelper.toMetaDataObjectName( Identifier.toIdentifier( "testentity", true ) );
		final String upperCaseName = identifierHelper.toMetaDataObjectName( Identifier.toIdentifier("TESTENTITY", true ) );
		final String mixedCaseName = identifierHelper.toMetaDataObjectName( Identifier.toIdentifier("TESTentity", true ) );
		if ( lowerCaseName.equals( upperCaseName ) ||
				lowerCaseName.equals( mixedCaseName ) ||
				upperCaseName.equals( mixedCaseName ) ) {
			StandardServiceRegistryBuilder.destroy( ssr );
			skipTest = true;
		}
	}

	@After
	public void tearsDown() {
		if ( skipTest ) {
			return;
		}
		new SchemaExport().setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.drop( EnumSet.of( TargetType.DATABASE ), metadata );
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testSchemaUpdateAndValidation() throws Exception {
		if ( skipTest ) {
			SkipLog.reportSkip( "skipping test because quoted names are not case-sensitive." );
			return;
		}

		new SchemaUpdate().setHaltOnError( true )
				.execute( EnumSet.of( TargetType.DATABASE ), metadata );

		new SchemaValidator().validate( metadata );

		new SchemaUpdate().setHaltOnError( true )
				.setOutputFile( output.getAbsolutePath() )
				.setFormat( false )
				.execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ), metadata );

		final String fileContent = new String( Files.readAllBytes( output.toPath() ) );
		assertThat( "The update output file should be empty", fileContent, is( "" ) );
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
