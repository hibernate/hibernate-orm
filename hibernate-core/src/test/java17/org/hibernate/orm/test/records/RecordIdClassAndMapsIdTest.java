/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.records;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REMOVE;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		RecordIdClassAndMapsIdTest.UserAuthorityEntity.class,
		RecordIdClassAndMapsIdTest.UserAuthorityId.class,
		RecordIdClassAndMapsIdTest.UserEntity.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-18062" )
public class RecordIdClassAndMapsIdTest {
	@Test
	public void testMapping(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final UserEntity ue = new UserEntity();
			ue.setName( "user_1" );
			final UserAuthorityEntity uae = new UserAuthorityEntity();
			ue.addUserAuthority( uae );
			uae.setUser( ue );
			uae.setAuthority( "auth_1" );
			session.persist( ue );
		} );
	}

	@Entity( name = "UserAuthorityEntity" )
	@IdClass( UserAuthorityId.class )
	static class UserAuthorityEntity {
		@Id
		private Long userId;

		@Id
		private String authority;

		@ManyToOne
		@MapsId( "userId" )
		@PrimaryKeyJoinColumn( name = "user_id" )
		private UserEntity user;

		public void setUser(UserEntity user) {
			this.user = user;
		}

		public void setAuthority(String authority) {
			this.authority = authority;
		}
	}

	@Embeddable
	record UserAuthorityId(Long userId, String authority) {
	}

	@Entity( name = "UserEntity" )
	static class UserEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToMany( cascade = { PERSIST, MERGE, REMOVE }, mappedBy = "user", orphanRemoval = true )
		private Set<UserAuthorityEntity> userAuthorities = new HashSet<>();

		public void setName(String name) {
			this.name = name;
		}

		public void addUserAuthority(UserAuthorityEntity userAuthority) {
			this.userAuthorities.add( userAuthority );
		}
	}
}
