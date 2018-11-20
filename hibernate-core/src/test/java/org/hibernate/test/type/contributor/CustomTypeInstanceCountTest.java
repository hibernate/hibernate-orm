/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.contributor;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-13105" )
public class CustomTypeInstanceCountTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { CorporateUser.class };
	}

	@Override
	public void buildEntityManagerFactory() {
		ArraySpyType.resetInstanceCount();
		assertEquals( 0, ArraySpyType.getInstanceCount() );
		super.buildEntityManagerFactory();
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			CorporateUser user = new CorporateUser();
			user.setUserName( "Vlad" );
			entityManager.persist( user );

			user.getEmailAddresses().add( "vlad@hibernate.info" );
			user.getEmailAddresses().add( "vlad@hibernate.net" );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			List<CorporateUser> users = entityManager.createQuery(
				"select u from CorporateUser u", CorporateUser.class )
			.getResultList();

			assertEquals( 1, users.size() );
		} );

		assertEquals( 1, ArraySpyType.getInstanceCount() );
	}

	@Entity(name = "CorporateUser")
	@TypeDef(typeClass = ArraySpyType.class, defaultForType = Array.class)
	public static class CorporateUser {

		@Id
		private String userName;

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

