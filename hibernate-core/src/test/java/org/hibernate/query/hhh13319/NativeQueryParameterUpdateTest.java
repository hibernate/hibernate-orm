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

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Rabin Banerjee
 */
public class NativeQueryParameterUpdateTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
			Person.class
		};
	}

	@Test
	public void testhhh13319() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Query query = entityManager.createNativeQuery(
				"SELECT * from person where id in (:ids)");
			query.setParameter("ids",Arrays.asList(1,2,5,10));
			query.getResultList();
			
			//Now query with another set of ids having more length than last
			query.setParameter("ids",Arrays.asList(15,278,58,110,98,53,29,25));
			query.getResultList();

			//Now query with another set of ids having less length than last
			query.setParameter("ids",Arrays.asList(91,26));
			query.getResultList();
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
