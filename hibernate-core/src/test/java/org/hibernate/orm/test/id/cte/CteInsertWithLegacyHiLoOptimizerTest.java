/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.cte;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.enhanced.LegacyHiLoAlgorithmOptimizer;
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
 * Tests sequence ID conflicts between entity persists and CTE batch inserts with {@link LegacyHiLoAlgorithmOptimizer},
 * ensuring proper ID allocation and prevention of duplicates across both operations.
 *
 * @author Kowsar Atazadeh
 */
@JiraKey("HHH-18818")
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.PREFERRED_POOLED_OPTIMIZER, value = "legacy-hilo"))
@DomainModel(annotatedClasses = Dummy.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCteInsertStrategy.class)
public class CteInsertWithLegacyHiLoOptimizerTest {
	@Test
	void test(SessionFactoryScope scope) {
		// 7 rows inserted with IDs 6-12
		// Database sequence calls:
		// - First returns 1 (allocates IDs 6-11)
		// - Second returns 2 (allocates IDs 12-17)
		// IDs 13-17 reserved from current allocation
		scope.inTransaction( session -> {
			for ( var id = 6; id <= 12; id++ ) {
				Dummy d = new Dummy( "d" + id );
				session.persist( d );
				assertEquals( id, d.getId() );
			}
		} );

		// 7 rows inserted with IDs 18-22, 24-25
		// Database sequence calls:
		// - First returns 3 (allocates IDs 18-22)
		// - Second returns 4 (allocates IDs 24-28)
		// Note: ID 23 skipped due to different batch sizes between CTE (5) and optimizer (6)
		// IDs 26-28 reserved from current allocation
		scope.inTransaction( session -> {
			session.createMutationQuery( "INSERT INTO Dummy (name) SELECT d.name FROM Dummy d" ).
					executeUpdate();
			var inserted = session.createSelectionQuery(
							"SELECT d.id FROM Dummy d WHERE d.id > 12 ORDER BY d.id", Long.class )
					.getResultList();
			assertEquals( 7, inserted.size() );

			int i = 0;
			for ( int id = 18; id <= 22; id++, i++ ) {
				assertEquals( id, inserted.get( i ) );
			}
			for ( int id = 24; id <= 25; id++, i++ ) {
				assertEquals( id, inserted.get( i ) );
			}
		} );

		// 8 rows inserted with IDs 13-17, 30-32
		// Using previously reserved IDs 13-17
		// Database sequence call returns 5 (allocates IDs 30-35)
		// IDs 33-35 reserved from current allocation
		scope.inTransaction( session -> {
			for ( var id = 13; id <= 17; id++ ) {
				Dummy d = new Dummy( "d" + id );
				session.persist( d );
				assertEquals( id, d.getId() );
			}

			for ( var id = 30; id <= 32; id++ ) {
				Dummy d = new Dummy( "d" + id );
				session.persist( d );
				assertEquals( id, d.getId() );
			}
		} );
	}
}
