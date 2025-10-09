/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.contributor;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Type;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey( value = "HHH-11409" )
@DomainModel(annotatedClasses = ArrayCustomTypeTest.CorporateUser.class)
@SessionFactory
public class ArrayCustomTypeTest {
	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@BeforeEach
	void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			CorporateUser user = new CorporateUser();
			user.setUserName( "Vlad" );
			session.persist( user );

			user.getEmailAddresses().add( "vlad@hibernate.info" );
			user.getEmailAddresses().add( "vlad@hibernate.net" );
		} );
	}

	@Test
	public void test(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			List<CorporateUser> users = session.createQuery(
					"select u from CorporateUser u where u.emailAddresses = :address",
							CorporateUser.class )
					.setParameter( "address", new Array() )
					.getResultList();

			assertTrue( users.isEmpty() );
		} );
	}

	@Test
	public void testNativeSQL(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			//noinspection unchecked
			List<Array> emails = session.createNativeQuery(
							"select u.emailAddresses from CorporateUser u where u.userName = :name" )
					.setParameter( "name", "Vlad" )
					.getResultList();

			assertEquals( 1, emails.size() );
		} );
	}

	@Entity(name = "CorporateUser")
	public static class CorporateUser {

		@Id
		private String userName;

		@Basic
		@Type( ArrayType.class )
		private Array emailAddresses = new Array();

		public String getUserName() {
			return userName;
		}

		public void setUserName(String userName) {
			this.userName = userName;
		}

		public Array getEmailAddresses() {
			return emailAddresses;
		}
	}


}
