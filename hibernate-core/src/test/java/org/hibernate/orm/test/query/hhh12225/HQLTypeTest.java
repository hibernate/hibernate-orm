/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.query.hhh12225;

import java.util.List;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


@TestForIssue(jiraKey = "HHH-12225")
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/query/hhh12225/Contract.hbm.xml",
				"org/hibernate/orm/test/query/hhh12225/Vehicle.hbm.xml"
		}
)
@SessionFactory
public class HQLTypeTest {

	@Test
	public void test(SessionFactoryScope scope) {
		VehicleContract contract = scope.fromTransaction( session -> {
			VehicleContract firstCotract = null;
			for ( long i = 0; i < 10; i++ ) {
				VehicleContract vehicleContract = new VehicleContract();
				Vehicle vehicle1 = new Vehicle();
				vehicle1.setContract( vehicleContract );
				VehicleTrackContract vehicleTrackContract = new VehicleTrackContract();
				Vehicle vehicle2 = new Vehicle();
				vehicle2.setContract( vehicleTrackContract );

				session.save( vehicle1 );
				session.save( vehicle2 );
				session.save( vehicleContract );
				session.save( vehicleTrackContract );
				if ( i == 0 ) {
					firstCotract = vehicleContract;
				}
			}
			return firstCotract;
		} );

		scope.inTransaction( session -> {
			List workingResults = session.createQuery(
							"select rootAlias.id from Contract as rootAlias where rootAlias.id = :id" )
					.setParameter( "id", contract.getId() )
					.getResultList();

			assertFalse( workingResults.isEmpty() );
			Long workingId = (Long) workingResults.get( 0 );
			assertEquals( contract.getId(), workingId );

			List failingResults = session.createQuery(
							"select rootAlias.id, type(rootAlias) from Contract as rootAlias where rootAlias.id = :id" )
					.setParameter( "id", contract.getId() )
					.getResultList();

			assertFalse( failingResults.isEmpty() );
			Long failingId = (Long) ( (Object[]) failingResults.get( 0 ) )[0];
			assertEquals( contract.getId(), failingId );
		} );
	}
}
