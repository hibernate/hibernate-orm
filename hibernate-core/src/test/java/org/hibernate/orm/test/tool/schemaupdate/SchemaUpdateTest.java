/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.naming.Identifier;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.SkipLog;
import org.hibernate.testing.junit5.SkipForDialect;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
@SkipForDialect(dialectClass = SQLServerDialect.class)
public class SchemaUpdateTest extends BaseSchemaUnitTestCase {

	private boolean skipTest;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				LowercaseTableNameEntity.class
				, TestEntity.class
				, UppercaseTableNameEntity.class
				, MixedCaseTableNameEntity.class
				, Match.class
				, InheritanceRootEntity.class
				, InheritanceChildEntity.class
				, InheritanceSecondChildEntity.class
		};
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED, "true" );
	}

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	public void afterServiceRegistryCreation(StandardServiceRegistry standardServiceRegistry) {

		// Databases that use case-insensitive quoted identifiers need to be skipped.
		// The following checks will work for checking those dialects that store case-insensitive
		// quoted identifiers as upper-case or lower-case. It does not work for dialects that
		// store case-insensitive identifiers in mixed case (like SQL Server).
		final IdentifierHelper identifierHelper = standardServiceRegistry.getService( JdbcEnvironment.class )
				.getIdentifierHelper();
		final String lowerCaseName = identifierHelper.toMetaDataObjectName( Identifier.toIdentifier(
				"testentity",
				true
		) );
		final String upperCaseName = identifierHelper.toMetaDataObjectName( Identifier.toIdentifier(
				"TESTENTITY",
				true
		) );
		final String mixedCaseName = identifierHelper.toMetaDataObjectName( Identifier.toIdentifier(
				"TESTentity",
				true
		) );
		if ( lowerCaseName.equals( upperCaseName ) ||
				lowerCaseName.equals( mixedCaseName ) ||
				upperCaseName.equals( mixedCaseName ) ) {
			skipTest = true;
		}
	}

	@SchemaTest
	public void testSchemaUpdateAndValidation(SchemaScope schemaScope) throws Exception {
		if ( skipTest ) {
			SkipLog.reportSkip( "skipping test because quoted names are not case-sensitive." );
			return;
		}

		schemaScope.withSchemaUpdate( schemaUpdate ->
								  schemaUpdate.setHaltOnError( true ).execute( EnumSet.of( TargetType.DATABASE ) ) );

		schemaScope.withSchemaValidator( schemaValidator -> schemaValidator.validate() );

		schemaScope.withSchemaUpdate( schemaUpdate ->
								  schemaUpdate.setHaltOnError( true )
										  .setFormat( false )
										  .execute( EnumSet.of( TargetType.DATABASE, TargetType.SCRIPT ) ) );

		final String fileContent = getSqlScriptOutputFileContent();
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
	@Table(name = "`TESTentity`", indexes = {
			@Index(name = "index1", columnList = "`FieLd1`"),
			@Index(name = "Index2", columnList = "`FIELD_2`")
	})
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
