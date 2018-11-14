/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.contributor;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.transaction.Transactional;

import org.hibernate.Session;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-13103" )
public class ArrayTypePropertiesTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { CorporateUser.class };
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( "hibernate.type.array.config", new Integer[]{1, 2, 3} );
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
	}

	@Test
	public void testConfigurationSettings() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			SharedSessionContractImplementor session = entityManager.unwrap( SharedSessionContractImplementor.class );

			CorporateUser corporateUser = entityManager.find( CorporateUser.class, "Vlad" );
			PersistenceContext persistenceContext = session.getPersistenceContext();
			EntityPersister entityPersister = persistenceContext.getEntry( corporateUser ).getPersister();
			ArrayType arrayType = (ArrayType) entityPersister.getPropertyType( "emailAddresses" );

			Map<String, Object> settings = arrayType.getSettings();
			Integer[] arrayConfig = (Integer[]) settings.get( "hibernate.type.array.config" );
			assertNotNull( arrayConfig );
			assertArrayEquals( new Integer[]{1, 2, 3}, arrayConfig );
		} );
	}

	@Entity(name = "CorporateUser")
	@TypeDef( typeClass = ArrayType.class, defaultForType = Array.class )
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

