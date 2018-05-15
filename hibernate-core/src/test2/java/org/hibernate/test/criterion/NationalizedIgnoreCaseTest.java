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
import org.hibernate.annotations.Nationalized;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.PostgreSQL81Dialect;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford (aka Naros)
 */
@TestForIssue( jiraKey = "HHH-8657" )
@SkipForDialect(value = DB2Dialect.class, comment = "DB2 jdbc driver doesn't support setNString")
@SkipForDialect(value = PostgreSQL81Dialect.class, comment = "PostgreSQL jdbc driver doesn't support setNString")
public class NationalizedIgnoreCaseTest extends BaseCoreFunctionalTestCase {
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { User.class };
	}
	
	@Test
	public void testIgnoreCaseCriteria() {

		User user1 = new User(1, "Chris");
		User user2 = new User(2, "Steve");

		doInHibernate( this::sessionFactory, session -> {
			session.save(user1);
			session.save(user2);
		} );

		doInHibernate( this::sessionFactory, session -> {
			Criteria criteria = session.createCriteria(User.class);
			criteria.add(Restrictions.eq("name", user1.getName().toLowerCase()).ignoreCase());
			assertEquals(1, criteria.list().size());
		} );
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