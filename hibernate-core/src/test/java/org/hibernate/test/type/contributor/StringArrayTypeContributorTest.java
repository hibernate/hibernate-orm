/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.contributor;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.collection.custom.basic.MyList;
import org.hibernate.test.type.array.StringArrayType;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-11409" )
@RequiresDialect(H2Dialect.class)
public class StringArrayTypeContributorTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { CorporateUser.class };
	}

	@Override
	protected void configureMetadataBuilder(MetadataBuilder metadataBuilder) {
		metadataBuilder.applyBasicType(
			StringArrayType.INSTANCE
		);
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Override
	protected void prepareTest() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			CorporateUser user = new CorporateUser();
			user.setUserName( "Vlad" );
			session.persist( user );

			user.setEmailAddresses( new String[] {
				"vlad@hibernate.info",
				"vlad@hibernate.net",
			} );
		} );
	}

	@Test
	public void testJPQL() {
		doInHibernate( this::sessionFactory, session -> {
			List<String[]> emails = session.createQuery(
				"select u.emailAddresses from CorporateUser u where u.userName = :name" )
			.setParameter( "name", "Vlad" )
			.getResultList();

			assertEquals( 1, emails.size() );
			assertArrayEquals(
					new String[] {
						"vlad@hibernate.info",
						"vlad@hibernate.net",
					},
					emails.get( 0 )
			);
		} );
	}

	@Test
	public void testNativeSQL() {
		doInHibernate( this::sessionFactory, session -> {
			List<String[]> emails = session.createNativeQuery(
				"select u.emailAddresses from CorporateUser u where u.userName = :name" )
			.setParameter( "name", "Vlad" )
			.getResultList();

			assertEquals( 1, emails.size() );
		} );
	}

	@Entity(name = "CorporateUser")
	@TypeDef( name = "string-array", typeClass = StringArrayType.class)
	public static class CorporateUser {

		@Id
		private String userName;

		@Type(type = "string-array")
		@Column(columnDefinition = "ARRAY(2)")
		private String[] emailAddresses;

		public String getUserName() {
			return userName;
		}

		public void setUserName(String userName) {
			this.userName = userName;
		}

		public String[] getEmailAddresses() {
			return emailAddresses;
		}

		public void setEmailAddresses(String[] emailAddresses) {
			this.emailAddresses = emailAddresses;
		}
	}


}

