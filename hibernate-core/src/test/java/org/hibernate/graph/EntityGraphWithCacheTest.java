package org.hibernate.graph;

import java.util.Collections;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.Table;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * @author Nathan Xu
 */
public class EntityGraphWithCacheTest extends BaseEntityManagerFunctionalTestCase {

	private Long passportId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Passport.class, Owner.class };
	}

	@Before
	public void setUp() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Owner owner = new Owner();

		Passport passport = new Passport();
		passport.setOwner(owner);

		em.persist(passport);

		passportId = passport.getId();

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13795" )
	public void test() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		// populate the entity in first-level cache
		em.find(Passport.class, passportId);

		EntityGraph<?> entityGraph = em.getEntityGraph("passport-entity-graph");
		Map<String, Object> props = Collections.singletonMap(GraphSemantic.FETCH.getJpaHintName(), entityGraph);
		Passport passport = em.find(Passport.class, passportId, props);

		assertFalse(passport.getOwner() instanceof HibernateProxy);

		em.getTransaction().commit();
		em.close();
	}

	@NamedEntityGraph(
		name = "passport-entity-graph",
		attributeNodes = {
			@NamedAttributeNode("owner")
		}
	)
	@Entity
	@Table
	public static class Passport {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		private Owner owner;

		public Long getId() {
			return id;
		}

		public Owner getOwner() {
			return owner;
		}

		public void setOwner(Owner owner) {
			this.owner = owner;
		}
	}

	@Entity
	@Table
	public static class Owner {
		@Id
		@GeneratedValue
		private Long id;

		public Long getId() {
			return id;
		}
	}
}
