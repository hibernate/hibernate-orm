/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.sequences;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.orm.test.annotations.id.sequences.entities.Planet;
import org.hibernate.orm.test.annotations.id.sequences.entities.PlanetCheatSheet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for enum type as id.
 *
 * @author Hardy Ferentschik
 */
@JiraKey(value = "ANN-744")
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
					PlanetCheatSheet mercuryFromDb = session.get(
							PlanetCheatSheet.class,
							mercury.getPlanet()
					);
					assertNotNull( mercuryFromDb );
					session.remove( mercuryFromDb );
				}
		);

		scope.inTransaction(
				session -> {
					PlanetCheatSheet mercuryFromDb = session.get( PlanetCheatSheet.class, Planet.MERCURY );
					assertNull( mercuryFromDb );
				}
		);
	}

}
