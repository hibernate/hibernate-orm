/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.test.c3p0;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests that when using cached prepared statement with batching enabled doesn't bleed over into new transactions.
 *
 * @author Shawn Clowater
 */
@JiraKey(value = "HHH-7193")
@SkipForDialect(dialectClass = SybaseASEDialect.class,
		reason = "JtdsConnection.isValid not implemented")
@SkipForDialect(dialectClass = SQLServerDialect.class,
		reason = "started failing after upgrade to c3p0 0.10")
@ServiceRegistry
@DomainModel( annotatedClasses = IrrelevantEntity.class)
@SessionFactory
public class StatementCacheTest {
	@Test
	public void testStatementCaching(SessionFactoryScope factoryScope) {
		factoryScope.inSession( (session) -> {
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
				fail( "Validation exception did not occur" );
			}
			catch (Exception e) {
				//this is expected roll the transaction back
				session.getTransaction().rollback();
			}
		} );

		factoryScope.inTransaction( session -> {
			//save a new entity and commit it
			IrrelevantEntity irrelevantEntity = new IrrelevantEntity();
			irrelevantEntity.setName( "valid 2" );
			session.persist( irrelevantEntity );
			session.flush();
		} );

		// only one entity should have been inserted to the database
		// (if the statement in the cache wasn't cleared then it would have inserted both entities)
		factoryScope.inTransaction( session -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<IrrelevantEntity> criteria = criteriaBuilder.createQuery( IrrelevantEntity.class );
			criteria.from( IrrelevantEntity.class );
			List<IrrelevantEntity> results = session.createQuery( criteria ).list();

			assertThat( results ).hasSize( 1 );
		} );
	}
}
