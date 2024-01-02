/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.dialect;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.HANADialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HANADialectTestCase extends BaseUnitTestCase {
	@Test
	public void testSqlGeneratedForIdentityInsertNoColumns() {
		ServiceRegistryScope.using(
				() -> ServiceRegistryUtil.serviceRegistryBuilder()
						.applySetting( AvailableSettings.DIALECT, HANADialect.class )
						.build(),
				registryScope -> {
					final StandardServiceRegistry registry = registryScope.getRegistry();
					final MetadataSources metadataSources = new MetadataSources( registry );
					metadataSources.addAnnotatedClass( EntityWithIdentity.class );

					String errorMessage = assertThrows( MappingException.class, () -> {
						try ( SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) metadataSources.buildMetadata().buildSessionFactory() ) {
							// Nothing to do, we expect an exception
						}
					} ).getMessage();
					assertThat( errorMessage )
							.matches( "The INSERT statement for table \\[EntityWithIdentity\\] contains no column, and this is not supported by \\[" + HANADialect.class.getName() + ", version: [\\d\\.]+\\]" );
				}
		);
	}

	/**
	 * Intentionally one of those silly cases where a table has only an id column.
	 * Here especially, since it is an IDENTITY the insert will have no columns at all.
	 */
	@Entity( name = "EntityWithIdentity" )
	@Table( name = "EntityWithIdentity" )
	public static class EntityWithIdentity {
	    @Id @GeneratedValue( strategy = GenerationType.IDENTITY )
	    private Integer id;

		private EntityWithIdentity() {
			// for use by Hibernate
		}

		public EntityWithIdentity(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13239")
	public void testLockWaitTimeout() {
		HANADialect dialect = new HANADialect();

		String sql = "select dummy from sys.dummy";

		LockOptions lockOptions = new LockOptions( LockMode.PESSIMISTIC_WRITE );
		lockOptions.setTimeOut( 2000 );

		Map<String, String[]> keyColumns = new HashMap<>();

		String sqlWithLock = dialect.applyLocksToSql( sql, lockOptions, new HashMap<>() );
		assertEquals( sql + " for update wait 2", sqlWithLock );

		lockOptions.setTimeOut( 0 );
		sqlWithLock = dialect.applyLocksToSql( sql, lockOptions, new HashMap<>() );
		assertEquals( sql + " for update nowait", sqlWithLock );

		lockOptions.setTimeOut( 500 );
		sqlWithLock = dialect.applyLocksToSql( sql, lockOptions, new HashMap<>() );
		assertEquals( sql + " for update wait 1", sqlWithLock );

		lockOptions.setTimeOut( 1500 );
		sqlWithLock = dialect.applyLocksToSql( sql, lockOptions, new HashMap<>() );
		assertEquals( sql + " for update wait 2", sqlWithLock );

		lockOptions.setAliasSpecificLockMode( "dummy", LockMode.PESSIMISTIC_READ );
		keyColumns.put( "dummy", new String[]{ "dummy" } );
		sqlWithLock = dialect.applyLocksToSql( sql, lockOptions, keyColumns );
		assertEquals( sql + " for update of dummy.dummy wait 2", sqlWithLock );
	}
}
