/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.c3p0;

import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;

/**
 * Tests that when using cached prepared statement with batching enabled doesn't bleed over into new transactions.
 *
 * @author Shawn Clowater
 */
@SkipForDialect(dialectClass = SybaseASEDialect.class,
		reason = "JtdsConnection.isValid not implemented")
public class StatementCacheTest extends BaseCoreFunctionalTestCase {
	@Test
	@JiraKey(value = "HHH-7193")
	@SkipForDialect(dialectClass = SQLServerDialect.class,
			reason = "started failing after upgrade to c3p0 0.10")
	public void testStatementCaching() {
		inSession(
				session -> {
					session.beginTransaction();

					//save 2 new entities, one valid, one invalid (neither should be persisted)
					IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
					irrelevantEntity.setName( "valid 1" );
					session.persist( irrelevantEntity );
					//name is required
					irrelevantEntity = new IrrelevantEntity();
					session.persist( irrelevantEntity );
					try {
						session.flush();
						Assert.fail( "Validation exception did not occur" );
					}
					catch (Exception e) {
						//this is expected roll the transaction back
						session.getTransaction().rollback();
					}
				}
		);

		inTransaction(
				session -> {
					//save a new entity and commit it
					IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
					irrelevantEntity.setName( "valid 2" );
					session.persist( irrelevantEntity );
					session.flush();
				}
		);

		// only one entity should have been inserted to the database
		// (if the statement in the cache wasn't cleared then it would have inserted both entities)
		inTransaction(
				session -> {
					CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					CriteriaQuery<IrrelevantEntity> criteria = criteriaBuilder.createQuery( IrrelevantEntity.class );
					criteria.from( IrrelevantEntity.class );
					List<IrrelevantEntity> results = session.createQuery( criteria ).list();

//		Criteria criteria = session.createCriteria( IrrelevantEntity.class );
//		List results = criteria.list();
					Assert.assertEquals( 1, results.size() );
				}
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ IrrelevantEntity.class };
	}
}
