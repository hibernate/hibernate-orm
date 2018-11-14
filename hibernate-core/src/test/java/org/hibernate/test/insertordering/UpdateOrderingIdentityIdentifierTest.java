/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.FlushModeType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.transaction.TransactionUtil;

/**
 * @author Chris Cranford
 */
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
@TestForIssue(jiraKey = "HHH-13053")
public class UpdateOrderingIdentityIdentifierTest extends BaseEntityManagerFunctionalTestCase {
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
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Animal.class, Zoo.class };
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( AvailableSettings.ORDER_UPDATES, "true" );
	}

	@Test
	public void testFailWithDelayedPostInsertIdentifier() {
		final Long zooId = TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final Zoo zoo = new Zoo();
			entityManager.persist( zoo );
			return zoo.getId();
		} );

		EntityManager entityManager = getOrCreateEntityManager();
		try {
			entityManager.setFlushMode( FlushModeType.COMMIT );
			Session session = entityManager.unwrap( Session.class );
			session.setHibernateFlushMode( FlushMode.MANUAL );

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
		catch ( Exception e ) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			entityManager.close();
		}
	}
}
