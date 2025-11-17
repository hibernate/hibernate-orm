/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.cte;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.enhanced.HiLoOptimizer;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests sequence ID conflicts between entity persists and CTE batch inserts with {@link HiLoOptimizer},
 * ensuring proper ID allocation and prevention of duplicates across both operations.
 *
 * @author Kowsar Atazadeh
 */
@JiraKey("HHH-18818")
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.PREFERRED_POOLED_OPTIMIZER, value = "hilo"))
@DomainModel(annotatedClasses = Dummy.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCteInsertStrategy.class)
public class CteInsertWithHiLoOptimizerTest {
	@Test
	void test(SessionFactoryScope scope) {
		// 7 rows inserted with IDs 1-7
		// Database sequence calls:
		// - First returns 1 (allocates IDs 1-5)
		// - Second returns 2 (allocates IDs 6-10)
		// IDs 8-10 reserved from current allocation
		scope.inTransaction( session -> {
			for ( var id = 1; id <= 7; id++ ) {
				Dummy d = new Dummy( "d" + id );
				session.persist( d );
				assertEquals( id, d.getId() );
			}
		} );

		// 7 rows inserted with IDs 11-17
		// Database sequence calls:
		// - First returns 3 (allocates IDs 11-15)
		// - Second returns 4 (allocates IDs 16-20)
		// IDs 18-20 reserved from current allocation
		scope.inTransaction( session -> {
			session.createMutationQuery( "INSERT INTO Dummy (name) SELECT d.name FROM Dummy d" ).
					executeUpdate();
			var inserted = session.createSelectionQuery(
							"SELECT d.id FROM Dummy d WHERE d.id > 7 ORDER BY d.id", Long.class )
					.getResultList();
			assertEquals( 7, inserted.size() );
			for ( int i = 0; i < inserted.size(); i++ ) {
				assertEquals( 11 + i, inserted.get( i ) );
			}
		} );

		// 5 rows inserted with IDs 8-10, 21-22
		// Database sequence call returns 5 (allocates IDs 21-25)
		// Using previously reserved IDs 8-10 and new allocation IDs 21-22
		// IDs 23-25 reserved from current allocation
		scope.inTransaction( session -> {
			for ( var id = 8; id <= 10; id++ ) {
				Dummy d = new Dummy( "d" + id );
				session.persist( d );
				assertEquals( id, d.getId() );
			}

			for ( var id = 21; id <= 22; id++ ) {
				Dummy d = new Dummy( "d" + id );
				session.persist( d );
				assertEquals( id, d.getId() );
			}
		} );
	}
}
