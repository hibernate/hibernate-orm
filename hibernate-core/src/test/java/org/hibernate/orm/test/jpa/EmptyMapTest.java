/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Réda Housni Alaoui
 */
public class EmptyMapTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EmptyMapTest.User.class, EmptyMapTest.Identity.class, EmptyMapTest.UserIdentity.class };
	}

	@Test
	@JiraKey(value = "HHH-18658")
	public void test() {
		EntityManager em = getOrCreateEntityManager();

		em.getTransaction().begin();

		User user = new User();
		em.persist( user );

		em.flush();
		em.clear();

		assertThat(em.find(User.class, user.id)).isNotNull();

		em.getTransaction().commit();
	}

	@Entity
	public static class User {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private int id;

		@OneToMany(
				mappedBy = "user",
				fetch = FetchType.EAGER,
				cascade = CascadeType.ALL,
				orphanRemoval = true)
		@MapKeyJoinColumn
		private final Map<Identity, UserIdentity> userIdentityByIdentity = new HashMap<>();
	}

	@Entity
	public static class Identity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private int id;
	}

	@Entity
	public static class UserIdentity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private int id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(nullable = false, updatable = false)
		private User user;

		@OneToOne(fetch = FetchType.EAGER)
		@JoinColumn(nullable = false, updatable = false)
		private Identity identity;
	}

}
