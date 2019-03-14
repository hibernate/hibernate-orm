/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.hhh13319;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Query;

import java.util.Arrays;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.hibernate.dialect.H2Dialect;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.RequiresDialect;

import static org.junit.Assert.assertEquals;

/**
 * @author Rabin Banerjee
 */
@TestForIssue( jiraKey = "HHH-13319" )
@RequiresDialect(H2Dialect.class)
public class NativeQueryParameterUpdateTest extends BaseEntityManagerFunctionalTestCase {

	public static final int MAX_COUNT = 20;

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
			Person.class
		};
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			for ( int i = 0; i < MAX_COUNT; i++ ) {
				Person person = new Person();
				person.setId( i );
				person.setName( String.format( "Person nr %d", i ) );

				entityManager.persist( person );
			}
		} );
	}

	@Test
	public void testhhh13319() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Query query = entityManager.createNativeQuery(
				"SELECT * from person where id in (:ids)");
			query.setParameter("ids",Arrays.asList(1,2,3,4));
			assertEquals(query.getResultList().size(),4);
			
			//Now query with another set of ids having more length than last
			query.setParameter("ids",Arrays.asList(6,7,8,9,10,11,12,13));
			assertEquals(query.getResultList().size(),8);

			//Now query with another set of ids having less length than last
			query.setParameter("ids",Arrays.asList(14,15));
			assertEquals(query.getResultList().size(),2);
		} );
	}

	@Entity
	@Table(name = "person")
	public static class Person {

		@Id
		private Integer id;

		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
