/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hhh14116;

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
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Beikov
 * @author Nathan Xu
 */
@JiraKey(value = "HHH-14116")
@DomainModel(
		annotatedClasses = {
				HHH14116Test.User.class,
				HHH14116Test.Group.class
		}
)
@SessionFactory
public class HHH14116Test {

	@Test
	public void testNoExceptionThrown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
					List<Group> resultList = session.createQuery(
									"SELECT g FROM User u JOIN u.groups g JOIN FETCH g.permissions JOIN FETCH g.tenant where u.id = ?1",
									Group.class )
							.setParameter( 1, 1L )
							.getResultList();
					assertThat( resultList ).hasSize( 0 );
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
