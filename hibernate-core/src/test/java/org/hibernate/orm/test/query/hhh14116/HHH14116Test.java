/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hhh14116;

import java.util.Set;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Christian Beikov
 * @author Nathan Xu
 */
@JiraKey( value = "HHH-14116" )
public class HHH14116Test extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { User.class, Group.class };
	}

	@Test
	public void testNoExceptionThrown() {
		doInJPA( this::sessionFactory, em -> {
				em.createQuery(
						"SELECT g FROM User u JOIN u.groups g JOIN FETCH g.permissions JOIN FETCH g.tenant where u.id = ?1", Group.class )
						.setParameter(1, 1L )
						.getResultList();
			}
		);
	}

	@Entity(name = "User")
	@Table(name = "usr_tbl")
	public static class User {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToMany
		private Set<Group> groups;

		@Enumerated(value = EnumType.STRING)
		@ElementCollection
		private Set<Permission> permissions;

	}

	@Entity(name = "Group")
	@Table(name = "grp_tbl")
	public static class Group {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		private User tenant;

		@Enumerated(value = EnumType.STRING)
		@ElementCollection
		private Set<Permission> permissions;

	}

	public enum Permission {
		READ,
		WRITE,
		EXECUTE;
	}
}
