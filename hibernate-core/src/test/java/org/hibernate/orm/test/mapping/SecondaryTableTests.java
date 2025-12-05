/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping;

import java.util.Date;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

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

	@Test
	public void updateOnSecondaryTableColumn(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new SimpleEntityWithSecondaryTables( 1, "test", null, null ) );
					session.flush();
					session.clear();
					session.createQuery( "update SimpleEntityWithSecondaryTables e set e.data = 'test'" ).executeUpdate();
					SimpleEntityWithSecondaryTables entity = session.find( SimpleEntityWithSecondaryTables.class, 1 );
					Assertions.assertEquals( "test", entity.data );
				}
		);
	}

	@Entity( name = "SimpleEntityWithSecondaryTables" )
	@Table( name = "root_table" )
	@SecondaryTable( name = "secondary_table1", pkJoinColumns = @PrimaryKeyJoinColumn( name = "s1_id" ) )
	@SecondaryTable( name = "secondary_table2", pkJoinColumns = @PrimaryKeyJoinColumn( name = "s2_id" ) )
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

		@Column( table = "secondary_table1" )
		public Date getDob() {
			return dob;
		}

		public void setDob(Date dob) {
			this.dob = dob;
		}

		@Column( table = "secondary_table2" )
		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}
}
