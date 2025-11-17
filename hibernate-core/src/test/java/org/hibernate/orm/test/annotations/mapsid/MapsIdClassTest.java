/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.mapsid;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.CascadeType.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@DomainModel(annotatedClasses ={MapsIdClassTest.User.class, MapsIdClassTest.UserAuthority.class})
public class MapsIdClassTest {

	@Test
	void test(SessionFactoryScope scope) {
		scope.inTransaction( s-> {
			User ue = new User();
			ue.setName("Gavin");
			UserAuthority uae = new UserAuthority();
			ue.addUserAuthority(uae);
			uae.setUser(ue);
			uae.setAuthority("God");
			s.persist(ue);
			s.flush();
			assertEquals( ue.id, uae.userId );
		});

	}

	static class UserAuthorityId {
		private Long userId;
		private String authority;
	}

	@Entity
	@Table(name = "users")
	static class User {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Long id;

		String name;

		String password;

		@Column(name = "is_enabled")
		boolean enabled;

		public void setName(String name) {
			this.name = name;
		}

		@OneToMany(
				cascade = {PERSIST, MERGE, REMOVE},
				mappedBy = "user",
				orphanRemoval = true)
		private Set<UserAuthority> userAuthorities = new HashSet<>();

		public void addUserAuthority(UserAuthority userAuthority) {
			this.userAuthorities.add(userAuthority);
		}
	}

	@Entity
	@IdClass(UserAuthorityId.class)
	@Table(name = "user_authorities")
	static class UserAuthority {

		@Id
		private Long userId;

		@Id
		private String authority;

		@ManyToOne
		@MapsId("userId")
		@PrimaryKeyJoinColumn(name = "user_id")
		private User user;

		public void setUser(User user) {
			this.user = user;
		}

		public void setAuthority(String authority) {
			this.authority = authority;
		}
	}
}
