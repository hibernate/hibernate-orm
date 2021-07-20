/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.contributor;

import java.util.List;
import java.util.Map;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.SqlResultSetMapping;

import org.hibernate.annotations.Type;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-11409")
public class ArrayTypeContributorTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { CorporateUser.class };
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put(
				EntityManagerFactoryBuilderImpl.METADATA_BUILDER_CONTRIBUTOR,
				(MetadataBuilderContributor) metadataBuilder ->
						metadataBuilder.applyTypes( (typeContributions, serviceRegistry) -> {
							typeContributions.contributeType( ArrayType.INSTANCE );
						} )
		);
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
					.setParameter( "address", new Array(), ArrayType.INSTANCE )
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

		doInJPA( this::entityManagerFactory, entityManager -> {
			List<CorporateUserDTO> emails = entityManager.createNativeQuery(
					"select " +
							"	u.userName as userName, " +
							"	u.emailAddresses as emailAddresses " +
							"from CorporateUser u " +
							"where u.userName = :name", "CorporateUserDTO" )
					.setParameter( "name", "Vlad" )
					.getResultList();

			assertEquals( 1, emails.size() );
		} );
	}

	@Entity(name = "CorporateUser")
	@SqlResultSetMapping(
			name = "CorporateUserDTO",
			classes = @ConstructorResult(
					targetClass = CorporateUserDTO.class,
					columns = {
							@ColumnResult(name = "userName", type = String.class),
							@ColumnResult(name = "emailAddresses", type = Array.class),
					}
			)

	)
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

	public static class CorporateUserDTO {

		private String userName;

		private Array emailAddresses = new Array();

		public CorporateUserDTO(String userName, Array emailAddresses) {
			this.userName = userName;
			this.emailAddresses = emailAddresses;
		}

		public String getUserName() {
			return userName;
		}

		public Array getEmailAddresses() {
			return emailAddresses;
		}
	}
}

