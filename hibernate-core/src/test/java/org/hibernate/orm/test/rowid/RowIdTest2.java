/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.rowid;

import java.math.BigDecimal;
import java.sql.Statement;

import org.hibernate.Transaction;
import org.hibernate.dialect.OracleDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;


/**
 * @author Gavin King
 */
@RequiresDialect(value = OracleDialect.class)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/rowid/Point.hbm.xml"
)
@SessionFactory(
		exportSchema = false

)
public class RowIdTest2 {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inSession(
				session -> session.doWork(
						connection -> {
							Statement st = session.getJdbcCoordinator().getStatementPreparer().createStatement();
							try {
								session.getJdbcCoordinator().getResultSetReturn().execute( st, "drop table Point" );
							}
							catch (Exception ignored) {
							}
							session.getJdbcCoordinator().getResultSetReturn().execute(
									st,
									"create table Point (\"x\" number(19,2) not null, \"y\" number(19,2) not null, description varchar2(255) )"
							);
							session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( st );
						}
				)
		);
	}

	@Test
	public void testRowId(SessionFactoryScope scope) {
		scope.inSession(
				s -> {
					try {
						Transaction t = s.beginTransaction();
						Point p = new Point( new BigDecimal( 1.0 ), new BigDecimal( 1.0 ) );
						s.persist( p );
						t.commit();
						s.clear();

						t = s.beginTransaction();
						CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
						CriteriaQuery<Point> criteria = criteriaBuilder.createQuery( Point.class );
						criteria.from( Point.class );
						p = s.createQuery( criteria ).uniqueResult();
//						p = (Point) s.createCriteria( Point.class ).uniqueResult();
						p.setDescription( "new desc" );
						t.commit();
						s.clear();

						t = s.beginTransaction();
						p = (Point) s.createQuery( "from Point" ).uniqueResult();
						p.setDescription( "new new desc" );
						t.commit();
						s.clear();

						t = s.beginTransaction();
						p = s.get( Point.class, p );
						p.setDescription( "new new new desc" );
						t.commit();
					}
					finally {
						if ( s.getTransaction().isActive() ) {
							s.getTransaction().rollback();
						}
					}
				}
		);
	}

}
