/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.cte;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.enhanced.PooledOptimizer;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests sequence ID conflicts between entity persists and CTE batch inserts with {@link PooledOptimizer},
 * ensuring proper ID allocation and prevention of duplicates across both operations.
 *
 * @author Kowsar Atazadeh
 */
@JiraKey("HHH-18818")
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.PREFERRED_POOLED_OPTIMIZER, value = "pooled"))
@DomainModel(annotatedClasses = Dummy.class)
public class CteInsertWithPooledOptimizerTest {
	@Test
	void test(SessionFactoryScope scope) {
		// 9 rows inserted with IDs 1-9
		// Database sequence calls:
		// - First returns 1 (allocates IDs 1-5)
		// - Second returns 6 (allocates IDs 6-10)
		// - Third returns 11 (allocates IDs 11-15)
		// IDs 10-11 reserved from current allocation
		scope.inTransaction( session -> {
			for ( var id = 1; id <= 9; id++ ) {
				Dummy d = new Dummy( "d" + id );
				session.persist( d );
				assertEquals( id, d.getId() );
			}
		} );

		// 9 rows inserted with IDs 12-20
		// Database sequence calls:
		// - First returns 16 (allocates IDs 16-20)
		// - Second returns 21 (allocates IDs 21-25)
		// IDs 21-25 reserved from current allocation
		scope.inTransaction( session -> {
			session.createMutationQuery( "INSERT INTO Dummy (name) SELECT d.name FROM Dummy d" ).
					executeUpdate();
			var inserted = session.createSelectionQuery(
							"SELECT d.id FROM Dummy d WHERE d.id > 9 ORDER BY d.id", Long.class )
					.getResultList();
			assertEquals( 9, inserted.size() );
			for ( int i = 0; i < inserted.size(); i++ ) {
				assertEquals( 12 + i, inserted.get( i ) );
			}
		} );

		// 2 rows inserted with IDs 10-11
		// Using previously reserved IDs
		scope.inTransaction( session -> {
			for ( var id = 10; id <= 11; id++ ) {
				Dummy d = new Dummy( "d" + id );
				session.persist( d );
				assertEquals( id, d.getId() );
			}
		} );

		// 1 row inserted with ID 22
		// Database sequence call returns 26 (allocates IDs 22-26)
		// IDs 23-26 reserved from current allocation
		scope.inTransaction( session -> {
			Dummy d22 = new Dummy( "d22" );
			session.persist( d22 );
			assertEquals( 22, d22.getId() );
		} );
	}
}
