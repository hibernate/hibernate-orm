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
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.collection.custom.basic.MyList;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-11409" )
public class ArrayTypeContributorTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { CorporateUser.class };
	}

	@Override
	protected Configuration constructAndConfigureConfiguration() {
		Configuration configuration = super.constructAndConfigureConfiguration();
		configuration.registerTypeContributor( (typeContributions, serviceRegistry) -> {
			typeContributions.contributeType( ArrayType.INSTANCE,
				new String[] {
					  MyList.class.getName(),
					  ArrayType.INSTANCE.getName()
				}
			);
		} );
		return configuration;
	}

	@Test
	public void test() {
		doInHibernate( this::sessionFactory, session -> {
			CorporateUser user = new CorporateUser();
			user.setUserName( "Vlad" );
			session.persist( user );

			user.getEmailAddresses().add( "vlad@hibernate.info" );
			user.getEmailAddresses().add( "vlad@hibernate.net" );
		} );
		doInHibernate( this::sessionFactory, session -> {
			List<CorporateUser> users = session.createQuery(
				"select u from CorporateUser u where u.emailAddresses = :address", CorporateUser.class )
			.setParameter( "address", new Array(), ArrayType.INSTANCE )
			.getResultList();

			assertTrue( users.isEmpty() );
		} );
	}

	@Entity(name = "CorporateUser")
	public static class CorporateUser {

		@Id
		private String userName;

		@Type(type = "comma-separated-array")
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

