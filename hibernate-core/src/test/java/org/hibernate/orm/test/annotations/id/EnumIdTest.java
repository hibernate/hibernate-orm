/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.id;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.orm.test.annotations.id.entities.Planet;
import org.hibernate.orm.test.annotations.id.entities.PlanetCheatSheet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/**
 * Tests for enum type as id.
 *
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "ANN-744")
@DomainModel(
		annotatedClasses = PlanetCheatSheet.class
)
@SessionFactory
public class EnumIdTest {

	@Test
	public void testEnumAsId(SessionFactoryScope scope) {
		PlanetCheatSheet mercury = new PlanetCheatSheet();
		scope.inTransaction(
				session -> {
					mercury.setPlanet( Planet.MERCURY );
					mercury.setMass( 3.303e+23 );
					mercury.setRadius( 2.4397e6 );
					mercury.setNumberOfInhabitants( 0 );
					session.persist( mercury );
				}
		);

		scope.inTransaction(
				session -> {
					PlanetCheatSheet mercuryFromDb = session.get( PlanetCheatSheet.class, mercury.getPlanet() );
					assertNotNull( mercuryFromDb );
					session.remove( mercuryFromDb );
				}
		);

		scope.inTransaction(
				session -> {
					PlanetCheatSheet mercuryFromDb = session.get(
							PlanetCheatSheet.class,
							Planet.MERCURY
					);
					assertNull( mercuryFromDb );
				}
		);

	}

}
