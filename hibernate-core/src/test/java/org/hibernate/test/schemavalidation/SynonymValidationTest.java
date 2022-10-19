/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemavalidation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DB297Dialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.hibernate.tool.schema.JdbcMetadaAccessStrategy;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.RequiresDialects;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Allows the BaseCoreFunctionalTestCase to create the schema using TestEntity.  The test method validates against an
 * identical entity, but using the synonym name.
 *
 * When SYNONYM are used, the GROUPED Strategy cannot be applied because when the tableNamePattern was not provided
 * java.sql.DatabaseMetaData#getColumns(...) Oracle implementation returns only the columns related with the synonym
 *
 * @author Brett Meyer
 */
@RequiresDialects({
		@RequiresDialect(Oracle9iDialect.class),
		@RequiresDialect(DB297Dialect.class)

})
public class SynonymValidationTest extends BaseNonConfigCoreFunctionalTestCase {
	private StandardServiceRegistry ssr;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {TestEntity.class};
	}

	@Before
	public void setUp() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			final String createStatement;
			if ( getDialect() instanceof Oracle9iDialect ) {
				createStatement = "CREATE SYNONYM test_synonym FOR test_entity";
			}
			else {
				createStatement = "CREATE ALIAS test_synonym FOR test_entity";
			}
			session.createNativeQuery( createStatement ).executeUpdate();

		} );
	}

	@After
	public void tearDown() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			final String dropStatement;
			if ( getDialect() instanceof Oracle9iDialect ) {
				dropStatement = "DROP SYNONYM test_synonym FORCE";
			}
			else {
				dropStatement = "DROP ALIAS test_synonym FOR TABLE";
			}
			session.createNativeQuery( dropStatement ).executeUpdate();
		});
	}

	@Test
	public void testSynonymUsingIndividuallySchemaValidator() {
		ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.ENABLE_SYNONYMS, "true" )
				.applySetting(
						AvailableSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY,
						JdbcMetadaAccessStrategy.INDIVIDUALLY
				)
				.build();
		try {
			final MetadataSources metadataSources = new MetadataSources( ssr );
			metadataSources.addAnnotatedClass( TestEntityWithSynonym.class );
			metadataSources.addAnnotatedClass( TestEntity.class );

			new SchemaValidator().validate( metadataSources.buildMetadata() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12406")
	public void testSynonymUsingDefaultStrategySchemaValidator() {
		// Hibernate should use JdbcMetadaAccessStrategy.INDIVIDUALLY when
		// AvailableSettings.ENABLE_SYNONYMS is true.
		ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.ENABLE_SYNONYMS, "true" )
				.build();
		try {
			final MetadataSources metadataSources = new MetadataSources( ssr );
			metadataSources.addAnnotatedClass( TestEntityWithSynonym.class );
			metadataSources.addAnnotatedClass( TestEntity.class );

			new SchemaValidator().validate( metadataSources.buildMetadata() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12406")
	public void testSynonymUsingGroupedSchemaValidator() {
		// Hibernate should use JdbcMetadaAccessStrategy.INDIVIDUALLY when
		// AvailableSettings.ENABLE_SYNONYMS is true,
		// even if JdbcMetadaAccessStrategy.GROUPED is specified.
		ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.ENABLE_SYNONYMS, "true" )
				.applySetting(
						AvailableSettings.HBM2DDL_JDBC_METADATA_EXTRACTOR_STRATEGY,
						JdbcMetadaAccessStrategy.GROUPED
				)
				.build();
		try {
			final MetadataSources metadataSources = new MetadataSources( ssr );
			metadataSources.addAnnotatedClass( TestEntityWithSynonym.class );
			metadataSources.addAnnotatedClass( TestEntity.class );

			new SchemaValidator().validate( metadataSources.buildMetadata() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity
	@Table(name = "test_entity")
	private static class TestEntity {
		@Id
		@GeneratedValue
		private Long id;

		@Column(nullable = false)
		private String key;

		@Column(name = "val")
		private String value;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	@Entity
	@Table(name = "test_synonym")
	private static class TestEntityWithSynonym {
		@Id
		@GeneratedValue
		private Long id;

		@Column(nullable = false)
		private String key;

		@Column(name = "val")
		private String value;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}
}
