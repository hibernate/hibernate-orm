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
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.HANAColumnStoreDialect;
import org.hibernate.engine.jdbc.mutation.MutationExecutor;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.spi.MutationExecutorService;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.sql.model.MutationOperationGroup;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.transaction.TransactionUtil2;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class HANADialectTestCase extends BaseUnitTestCase {
	@Test
	public void testSqlGeneratedForIdentityInsertNoColumns() {
		ServiceRegistryScope.using(
				() -> new StandardServiceRegistryBuilder()
						.applySetting( AvailableSettings.DIALECT, HANAColumnStoreDialect.class )
						.build(),
				(registryScope) -> {
					final StandardServiceRegistry registry = registryScope.getRegistry();
					final MetadataSources metadataSources = new MetadataSources( registry );
					metadataSources.addAnnotatedClass( EntityWithIdentity.class );

					try ( SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) metadataSources.buildMetadata().buildSessionFactory() ) {
						final PostInsertIdentityPersister entityDescriptor = (PostInsertIdentityPersister) sessionFactory.getRuntimeMetamodels()
								.getMappingMetamodel()
								.getEntityDescriptor( EntityWithIdentity.class );
						final MutationOperationGroup staticInsertGroup = ( (SingleTableEntityPersister) entityDescriptor ).getInsertCoordinator().getStaticInsertGroup();

						final MutationExecutorService mutationExecutorService = sessionFactory
								.getServiceRegistry()
								.getService( MutationExecutorService.class );

						TransactionUtil2.inTransaction(
								sessionFactory,
								(session) -> {
									final MutationExecutor mutationExecutor = mutationExecutorService.createExecutor(
											() -> null,
											staticInsertGroup,
											session
									);
									final PreparedStatementDetails statementDetails = mutationExecutor.getPreparedStatementDetails( "EntityWithIdentity" );
									assertThat( statementDetails.getSqlString() ).isEqualTo( "insert into EntityWithIdentity values ( )" );
								}
						);
					}
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
		HANAColumnStoreDialect dialect = new HANAColumnStoreDialect();

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
		assertEquals( sql + " for update nowait", sqlWithLock );

		lockOptions.setTimeOut( 1500 );
		sqlWithLock = dialect.applyLocksToSql( sql, lockOptions, new HashMap<>() );
		assertEquals( sql + " for update wait 1", sqlWithLock );

		lockOptions.setAliasSpecificLockMode( "dummy", LockMode.PESSIMISTIC_READ );
		keyColumns.put( "dummy", new String[]{ "dummy" } );
		sqlWithLock = dialect.applyLocksToSql( sql, lockOptions, keyColumns );
		assertEquals( sql + " for update of dummy.dummy wait 1", sqlWithLock );
	}
}
