/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idclass;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.hibernate.Transaction;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.TypedQuery;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				IdClassQueryTest.FederatedIdentityEntity.class,
				IdClassQueryTest.UserEntity.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-16387")
public class IdClassQueryTest {

	@Test
	public void testQuery(SessionFactoryScope scope) {
		UserEntity userEntity = new UserEntity();
		userEntity.setId( "1" );
		userEntity.setUsername( "user1" );

		scope.inSession(
				session -> {
					Transaction transaction = session.beginTransaction();
					try {

						FederatedIdentityEntity identityEntity = new FederatedIdentityEntity();
						identityEntity.setIdentityProvider( "idp" );
						identityEntity.setUser( userEntity );
						identityEntity.setUserId( userEntity.getId() );
						identityEntity.setUserName( userEntity.getUsername() );

						session.persist( userEntity );
						session.persist( identityEntity );

						transaction.commit();

						transaction.begin();

						TypedQuery<FederatedIdentityEntity> query = session.createQuery(
								"select link from FederatedIdentityEntity link where link.user = :user and link.identityProvider = :identityProvider",
								FederatedIdentityEntity.class
						);
						query.setParameter( "user", userEntity );
						query.setParameter( "identityProvider", "idp" );

						List<FederatedIdentityEntity> results = query.getResultList();

						assertThat( results.size() ).isEqualTo( 1 );

						FederatedIdentityEntity federatedIdentityEntity = results.get( 0 );
						assertThat( federatedIdentityEntity.getIdentityProvider() ).isEqualTo( "idp" );
						assertThat( federatedIdentityEntity.getUser() ).isSameAs( userEntity );
						assertThat( federatedIdentityEntity.getUserId() ).isEqualTo( userEntity.getId() );
						assertThat( federatedIdentityEntity.getUserName() ).isEqualTo( userEntity.getUsername() );

						transaction.commit();
					}
					catch (Exception e) {
						if ( transaction.isActive() ) {
							transaction.rollback();
						}
						throw e;
					}
				}
		);

		scope.inTransaction(
				session -> {
					TypedQuery<FederatedIdentityEntity> query = session.createQuery(
							"select link from FederatedIdentityEntity link where link.user = :user and link.identityProvider = :identityProvider",
							FederatedIdentityEntity.class
					);
					query.setParameter( "user", userEntity );
					query.setParameter( "identityProvider", "idp" );

					List<FederatedIdentityEntity> results = query.getResultList();

					assertThat( results.size() ).isEqualTo( 1 );

					FederatedIdentityEntity federatedIdentityEntity = results.get( 0 );
					assertThat( federatedIdentityEntity.getIdentityProvider() ).isEqualTo( "idp" );
					assertThat( federatedIdentityEntity.getUser() ).isEqualTo( userEntity );
					assertThat( federatedIdentityEntity.getUserId() ).isEqualTo( userEntity.getId() );
					assertThat( federatedIdentityEntity.getUserName() ).isEqualTo( userEntity.getUsername() );

				}
		);
	}

	@Entity(name = "UserEntity")
	public static class UserEntity {
		@Id
		protected String id;

		@Column(name = "USERNAME")
		protected String username;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			UserEntity that = (UserEntity) o;
			return Objects.equals( id, that.id ) && Objects.equals( username, that.username );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, username );
		}
	}

	@Entity(name = "FederatedIdentityEntity")
	@IdClass(Key.class)
	public static class FederatedIdentityEntity {

		@Id
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "USER_ID")
		private UserEntity user;

		@Id
		@Column(name = "IDENTITY_PROVIDER")
		protected String identityProvider;

		@Column(name = "FEDERATED_USER_ID")
		protected String userId;

		@Column(name = "FEDERATED_USERNAME")
		protected String userName;

		public UserEntity getUser() {
			return user;
		}

		public void setUser(UserEntity user) {
			this.user = user;
		}

		public String getIdentityProvider() {
			return identityProvider;
		}

		public void setIdentityProvider(String identityProvider) {
			this.identityProvider = identityProvider;
		}

		public String getUserId() {
			return userId;
		}

		public void setUserId(String userId) {
			this.userId = userId;
		}

		public String getUserName() {
			return userName;
		}

		public void setUserName(String userName) {
			this.userName = userName;
		}

	}

	public static class Key implements Serializable {

		protected UserEntity user;

		protected String identityProvider;

		public Key() {
		}

		public Key(UserEntity user, String identityProvider) {
			this.user = user;
			this.identityProvider = identityProvider;
		}

		public UserEntity getUser() {
			return user;
		}

		public String getIdentityProvider() {
			return identityProvider;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			Key key = (Key) o;

			if ( identityProvider != null ?
					!identityProvider.equals( key.identityProvider ) :
					key.identityProvider != null ) {
				return false;
			}
			if ( user != null ?
					!user.getId().equals( key.user != null ? key.user.getId() : null ) :
					key.user != null ) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = user != null ? user.getId().hashCode() : 0;
			result = 31 * result + ( identityProvider != null ? identityProvider.hashCode() : 0 );
			return result;
		}

		@Override
		public String toString() {
			return "FederatedIdentityEntity.Key [user=" + ( user != null ?
					user.getId() :
					null ) + ", identityProvider=" + identityProvider + "]";
		}
	}

}
