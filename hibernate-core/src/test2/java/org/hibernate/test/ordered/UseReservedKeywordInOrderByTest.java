/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.ordered;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Version;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
public class UseReservedKeywordInOrderByTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{Person.class, Location.class};
	}

	@Test
	public void testOrderBy(){
		try(Session s = openSession();){
			s.createQuery( "from Person p order by p.update" );
		}
	}

	@Test
	public void testMultipleOrderBy(){
		try(Session s = openSession();){
			s.createQuery( "from Person p order by p.name,p.update" );
		}
	}

	@Test
	public void testOrderByOfAssociationEntityField(){
		try(Session s = openSession();){
			s.createQuery( "from Person p order by p.location.update" );
		}
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private Integer id;

		private String name;

		@Column(name = "update_date")
		private Date update;

		@OneToOne
		Location location;
	}

	@Entity(name = "Location")
	public static class Location{
		@Id
		private Integer id;

		private String city;

		@Column(name = "update_date")
		private Date update;

	}
}
