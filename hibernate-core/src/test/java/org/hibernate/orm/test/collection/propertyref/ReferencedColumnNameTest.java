/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.propertyref;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Steve Ebersole
 */
@JiraKey(value = "HHH-565")
@DomainModel(
		annotatedClasses = {
				ReferencedColumnNameTest.User.class,
				ReferencedColumnNameTest.Mail.class
		}
)
@SessionFactory
public class ReferencedColumnNameTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testGet(SessionFactoryScope scope){
		User user = new User( "test" );
		scope.inTransaction(
				session -> {
					user.addMail( "test" );
					user.addMail( "test" );
					session.persist( user );
				}
		);

		scope.inTransaction(
				session -> {
					final User u = session.get( User.class, user.getId() );

					final Set<Mail> mail = u.getMail();
					assertTrue( Hibernate.isInitialized( mail ) );
					assertEquals( 2, mail.size() );

					final String alias = mail.iterator().next().getAlias();
					assertEquals( "test", alias );
				}
		);
	}

	@Entity(name = "User")
	@Table(name = "t_user")
	public static class User {

		@Id
		@GeneratedValue
		private Integer id;
		private String userid;

		@OneToMany(fetch = FetchType.EAGER,cascade = CascadeType.ALL,mappedBy = "user")
		@org.hibernate.annotations.Fetch(
				FetchMode.SELECT
		)

		private Set<Mail> mail = new HashSet();

		public User() {
		}

		public User(String userid) {
			this.userid = userid;
		}

		public Integer getId() {
			return id;
		}

		protected void setId(Integer id) {
			this.id = id;
		}

		public String getUserid() {
			return userid;
		}

		public void setUserid(String userid) {
			this.userid = userid;
		}

		public Set<Mail> getMail() {
			return mail;
		}

		private void setMail(Set<Mail> mail) {
			this.mail = mail;
		}

		public Mail addMail(String alias) {
			Mail mail = new Mail( alias, this );
			getMail().add( mail );
			return mail;
		}

		public void removeMail(Mail mail) {
			getMail().remove( mail );
		}
	}

	@Entity(name = "Mail")
	@Table(name = "t_mail")
	public static class Mail {

		@Id
		@GeneratedValue
		private Integer id;


		private String alias;

		@ManyToOne
		@JoinColumn(referencedColumnName = "userId")
		private User user;

		Mail() {
		}

		Mail(String alias, User user) {
			this.alias = alias;
			this.user = user;
		}

		public Integer getId() {
			return id;
		}

		protected void setId(Integer id) {
			this.id = id;
		}

		public String getAlias() {
			return alias;
		}

		public void setAlias(String alias) {
			this.alias = alias;
		}

		public User getUser() {
			return user;
		}

		public void setUser(User user) {
			this.user = user;
		}

	}


}
