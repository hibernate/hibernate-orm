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
import java.util.List;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.hibernate.dialect.H2Dialect;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.RequiresDialect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
				"SELECT * from person where id in (:ids)",Person.class);
			
			List<Integer> inp = Arrays.asList(1,2,3,4);
			doTest(query,inp);			
			
			//Now query with another set of ids having more length than last
			inp = Arrays.asList(6,7,8,9,10,11,12,13);
			doTest(query,inp);

			//Now query with another set of ids having less length than last
			inp = Arrays.asList(14,15);
			doTest(query,inp);

			//Again query with 2 element
			query.setParameter("ids",Arrays.asList(16,17));
			doTest(query,inp);

			//Again query with more element
                        query.setParameter("ids",Arrays.asList(13,14,19,20));
                        doTest(query,inp);

			//Again query with less element
                        query.setParameter("ids",Arrays.asList(12,17));
                        doTest(query,inp);

			//Again query with element which doesn not exist
                        query.setParameter("ids",Arrays.asList(MAX_COUNT+100,MAX_COUNT+200));
			assertEquals(query.getResultList().size(),0);

		} );
	}

	public void doTest(Query query,List<Integer> inp) {
		query.setParameter("ids",inp);
                List<Person> persons = query.getResultList();
       	        assertEquals(persons.size(),inp.size());
                persons.forEach(p->assertTrue(inp.contains(p.getId())));
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
