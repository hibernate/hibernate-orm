/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetoone;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;


@Jpa(
		annotatedClasses = {
				UnownedOneToOneWithInheritanceTest.Buchung.class,
				UnownedOneToOneWithInheritanceTest.Energiefluss.class,
				UnownedOneToOneWithInheritanceTest.Erzeugungsanlage.class,
				UnownedOneToOneWithInheritanceTest.Portfoliowirkung.class
		}
)
@Jira("https://hibernate.atlassian.net/browse/HHH-9499")
public class UnownedOneToOneWithInheritanceTest {

	@Test
	public void testModel(EntityManagerFactoryScope scope) {
		scope.inTransaction( s -> {
		} );
	}

	@Entity
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class Buchung {
		@Id
		private Long id;

		@ManyToOne
		private Energiefluss energiefluss;
	}

	@Entity
	@Inheritance(strategy = InheritanceType.JOINED)
	public static abstract class Energiefluss {
		@Id
		private Long id;

		@OneToMany(mappedBy = "energiefluss")
		private List<Buchung> buchungen = new ArrayList<>();
	}

	@Entity
	public static class Erzeugungsanlage extends Energiefluss {
		@OneToOne(mappedBy = "energiefluss")
		private Portfoliowirkung portfoliowirkung;
	}

	@Entity
	public static class Portfoliowirkung extends Buchung {
	}
}
