/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * @author Chris Cranford
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@JiraKey(value = "HHH-13053")
@Jpa(
		annotatedClasses = {
				UpdateOrderingIdentityIdentifierTest.Animal.class, UpdateOrderingIdentityIdentifierTest.Zoo.class
		},
		integrationSettings = @Setting(name = AvailableSettings.ORDER_UPDATES, value = "true")
)
public class UpdateOrderingIdentityIdentifierTest {

	@Test
	public void testFailWithDelayedPostInsertIdentifier(EntityManagerFactoryScope scope) {
		final Long zooId = scope.fromTransaction( entityManager -> {
			final Zoo zoo = new Zoo();
			entityManager.persist( zoo );
			return zoo.getId();
		} );

		scope.inEntityManager( entityManager -> {
			entityManager.setFlushMode( FlushModeType.COMMIT );
			Session session = entityManager.unwrap( Session.class );
			session.setHibernateFlushMode( FlushMode.MANUAL );

			try {
				entityManager.getTransaction().begin();

				final Zoo zooExisting = entityManager.find( Zoo.class, zooId );

				Zoo zoo = new Zoo();
				entityManager.persist( zoo );
				entityManager.flush();

				Animal animal1 = new Animal();
				animal1.setZoo( zoo );
				zooExisting.getAnimals().add( animal1 );

				Animal animal2 = new Animal();
				animal2.setZoo( zoo );
				zoo.getAnimals().add( animal2 );

				// When allowing delayed identity inserts, this flush would result in a failure due to
				// CollectionAction#compareTo using a DelayedPostInsertIdentifier object.
				entityManager.flush();

				entityManager.getTransaction().commit();
			}
			catch (Exception e) {
				if ( entityManager.getTransaction().isActive() ) {
					entityManager.getTransaction().rollback();
				}
				throw e;
			}
		} );
	}

	@Entity(name = "Zoo")
	public static class Zoo {
		private Long id;
		private String data;
		private List<Animal> animals = new ArrayList<>();

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "zoo", cascade = CascadeType.ALL)
		public List<Animal> getAnimals() {
			return animals;
		}

		public void setAnimals(List<Animal> animals) {
			this.animals = animals;
		}
	}

	@Entity(name = "Animal")
	public static class Animal {
		private Long id;
		private String name;
		private Zoo zoo;

		@Id
		@GeneratedValue(generator = "AnimalSeq")
		@GenericGenerator(name = "AniamlSeq", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
				@Parameter(name = "sequence_name", value = "ANIMAL_SEQ"),
				@Parameter(name = "optimizer", value = "pooled"),
				@Parameter(name = "increment_size", value = "50")
		})
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "ZOO_ID", nullable = false)
		public Zoo getZoo() {
			return zoo;
		}

		public void setZoo(Zoo zoo) {
			this.zoo = zoo;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
