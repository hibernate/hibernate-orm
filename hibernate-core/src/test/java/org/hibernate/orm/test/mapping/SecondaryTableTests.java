/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = SecondaryTableTests.SimpleEntityWithSecondaryTables.class )
@ServiceRegistry
@SessionFactory
public class SecondaryTableTests {

	@Test
	public void simpleTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select e from SimpleEntityWithSecondaryTables e" ).list();
				}
		);
	}

	@Entity( name = "SimpleEntityWithSecondaryTables" )
	@Table( name = "simple_w_secondary_tables0" )
	@SecondaryTable( name = "simple_w_secondary_tables1" )
	@SecondaryTable( name = "simple_w_secondary_tables2" )
	public static class SimpleEntityWithSecondaryTables {
		private Integer id;
		private String name;
		private Date dob;
		private String data;

		public SimpleEntityWithSecondaryTables() {
		}

		public SimpleEntityWithSecondaryTables(Integer id, String name, Date dob, String data) {
			this.id = id;
			this.name = name;
			this.dob = dob;
			this.data = data;
		}

		@Id
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

		@Column( table = "simple_w_secondary_tables1" )
		public Date getDob() {
			return dob;
		}

		public void setDob(Date dob) {
			this.dob = dob;
		}

		@Column( table = "simple_w_secondary_tables2" )
		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}
}
