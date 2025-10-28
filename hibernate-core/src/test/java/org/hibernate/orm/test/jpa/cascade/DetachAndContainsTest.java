/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade;

import java.util.ArrayList;
import java.util.Collection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static jakarta.persistence.CascadeType.DETACH;
import static jakarta.persistence.CascadeType.REMOVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
@Jpa(annotatedClasses = {
		DetachAndContainsTest.Mouth.class,
		DetachAndContainsTest.Tooth.class
})
public class DetachAndContainsTest {

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testDetach(EntityManagerFactoryScope scope) {
		Tooth tooth = new Tooth();
		Mouth mouth = new Mouth();
		scope.inTransaction(
				entityManager -> {
					entityManager.persist( mouth );
					entityManager.persist( tooth );
					tooth.mouth = mouth;
					mouth.teeth = new ArrayList<>();
					mouth.teeth.add( tooth );
				}
		);

		scope.inTransaction(
				entityManager -> {
					Mouth _mouth = entityManager.find( Mouth.class, mouth.id );
					assertNotNull( _mouth );
					assertEquals( 1, _mouth.teeth.size() );
					Tooth _tooth = _mouth.teeth.iterator().next();
					entityManager.detach( _mouth );
					assertFalse( entityManager.contains( _tooth ) );
				}
		);
	}

	@Entity(name = "Mouth")
	@Table(name = "mouth")
	public static class Mouth {
		@Id
		@GeneratedValue
		public Integer id;
		@OneToMany(mappedBy = "mouth", cascade = { DETACH, REMOVE } )
		public Collection<Tooth> teeth;
	}

	@Entity(name = "Tooth")
	@Table(name = "tooth")
	public static class Tooth {
		@Id
		@GeneratedValue
		public Integer id;
		public String type;
		@ManyToOne
		public Mouth mouth;
	}
}
