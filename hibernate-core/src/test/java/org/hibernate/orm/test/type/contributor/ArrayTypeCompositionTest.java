/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.contributor;

import java.util.List;

import org.hibernate.annotations.JavaType;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@JiraKey( value = "HHH-11409" )
public class ArrayTypeCompositionTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { CorporateUser.class };
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			CorporateUser user = new CorporateUser();
			user.setUserName( "Vlad" );
			entityManager.persist( user );

			user.getEmailAddresses().add( "vlad@hibernate.info" );
			user.getEmailAddresses().add( "vlad@hibernate.net" );
		} );
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<CorporateUser> users = entityManager.createQuery(
				"select u from CorporateUser u where u.emailAddresses = :address", CorporateUser.class )
			.unwrap( Query.class )
			.setParameter( "address", new Array() )
			.getResultList();

			assertTrue( users.isEmpty() );
		} );
	}

	@Test
	public void testNativeSQL() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Array> emails = entityManager.createNativeQuery(
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

		@JavaType( ArrayJavaType.class )
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
