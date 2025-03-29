/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.batch;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				RefreshAndBatchTest.User.class,
				RefreshAndBatchTest.UserInfo.class,
				RefreshAndBatchTest.Phone.class,
		}

)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "100")

		}
)
@JiraKey("HHH-18608")
@BytecodeEnhanced(runNotEnhancedAsWell = true)
public class RefreshAndBatchTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					UserInfo info = new UserInfo( "info" );
					Phone phone = new Phone( "123456" );
					info.addPhone( phone );
					User user = new User( 1L, "user1", info );
					session.persist( user );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete User" ).executeUpdate();
					session.createMutationQuery( "delete Phone" ).executeUpdate();
					session.createMutationQuery( "delete UserInfo" ).executeUpdate();
				}
		);
	}

	@Test
	public void testRefresh(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					User user = session.createQuery( "select u from User u where u.id = :id", User.class )
							.setParameter( "id", 1L )
							.getSingleResult();
					assertThat( Hibernate.isInitialized( user.getInfo() ) ).isFalse();
					session.refresh( user.getInfo() );
					assertThat( Hibernate.isInitialized( user.getInfo() ) ).isTrue();
				}
		);
	}

	@Entity(name = "User")
	@Table(name = "USER_TABLE")
	@BatchSize(size = 5)
	public static class User {

		@Id
		private Long id;

		@Column
		private String name;

		@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		@JoinColumn(name = "INFO_ID", referencedColumnName = "ID")
		private UserInfo info;

		public User() {
		}

		public User(long id, String name, UserInfo info) {
			this.id = id;
			this.name = name;
			this.info = info;
			info.user = this;
		}

		public long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public UserInfo getInfo() {
			return info;
		}
	}

	@Entity(name = "UserInfo")
	public static class UserInfo {
		@Id
		@GeneratedValue
		private Long id;

		@OneToOne(mappedBy = "info", fetch = FetchType.LAZY)
		private User user;

		private String info;

		@OneToMany(mappedBy = "info", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		private List<Phone> phoneList;

		public long getId() {
			return id;
		}

		public UserInfo() {
		}

		public UserInfo(String info) {
			this.info = info;
		}

		public User getUser() {
			return user;
		}

		public String getInfo() {
			return info;
		}

		public List<Phone> getPhoneList() {
			return phoneList;
		}

		public void addPhone(Phone phone) {
			if ( phoneList == null ) {
				phoneList = new ArrayList<>();
			}
			this.phoneList.add( phone );
			phone.info = this;
		}
	}

	@Entity(name = "Phone")
	public static class Phone {
		@Id
		@Column(name = "PHONE_NUMBER")
		private String number;

		@ManyToOne
		@JoinColumn(name = "INFO_ID")
		private UserInfo info;

		public Phone() {
		}

		public Phone(String number) {
			this.number = number;
		}

		public String getNumber() {
			return number;
		}

		public UserInfo getInfo() {
			return info;
		}
	}
}
