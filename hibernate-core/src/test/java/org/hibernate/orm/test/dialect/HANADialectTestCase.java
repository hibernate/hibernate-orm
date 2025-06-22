/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.HANADialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry
@DomainModel( annotatedClasses = HANADialectTestCase.EntityWithIdentity.class )
@RequiresDialect( HANADialect.class )
public class HANADialectTestCase {
	@Test
	public void testIdentityInsertNoColumns(DomainModelScope modelScope) {
		final MetadataImplementor domainModel = modelScope.getDomainModel();

		try ( SessionFactoryImplementor sessionFactory = domainModel.buildSessionFactory() ) {
			fail( "Expecting a MappingException" );
		}
		catch (MappingException expected) {
			assertThat( expected.getMessage() )
					.matches( "The INSERT statement for table \\[EntityWithIdentity\\] contains no column, and this is not supported by \\[" + HANADialect.class.getName() + ", version: [\\d\\.]+\\]" );
		}
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
	@JiraKey(value = "HHH-13239")
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
