/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criterion;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Nationalized;
import org.hibernate.criterion.Restrictions;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Chris Cranford (aka Naros)
 */
@TestForIssue( jiraKey = "HHH-8657" )
public class NationalizedIgnoreCaseTest extends BaseCoreFunctionalTestCase {
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { User.class };
	}
	
	@Test
	public void testIgnoreCaseCriteria() {
		
		Session session = openSession();
		Transaction trx = session.getTransaction();
		
		User user1 = new User(1, "Chris");
		User user2 = new User(2, "Steve");
		
		// persist the records
		trx.begin();		
		session.save(user1);
		session.save(user2);
		trx.commit();
		session.close();
		
		session = openSession();
		trx = session.getTransaction();
		
		Criteria criteria = session.createCriteria(User.class);
		criteria.add(Restrictions.eq("name", user1.getName().toLowerCase()).ignoreCase());
		assertEquals(1, criteria.list().size());
			
	}
	
	@Entity(name = "User")
	@Table(name = "users")
	public static class User {
		
		private Integer id;
		private String name;
		
		public User() {
			
		}
		
		public User(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
		
		@Id
		public Integer getId() {
			return id;
		}
		public void setId(Integer id) {
			this.id = id;
		}
		
		@Nationalized
		@Column(nullable = false)
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		
	}
}