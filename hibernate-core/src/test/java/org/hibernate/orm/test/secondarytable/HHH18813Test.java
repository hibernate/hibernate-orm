/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.secondarytable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SecondaryTable;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * Test for a Bugfix described in HHH-18813.
 * The CteUpdateHandler generated an Insert-Query,
 * which contained columns that do not exist in the target table.
 *
 * @author Peter Bambazek
 */
@JiraKey(value = "HHH-18813")
@DomainModel(
		annotatedClasses = {HHH18813Test.SecondaryTableEntitySub.class, HHH18813Test.SecondaryTableEntityBase.class})
@SessionFactory
class HHH18813Test {

	@Test
	void hhh18813Test(SessionFactoryScope scope) {

		// prepare
		scope.inTransaction( session -> {
			SecondaryTableEntitySub entitySub = new SecondaryTableEntitySub();
			entitySub.setB( 111L );
			entitySub.setC( 222L );
			session.persist( entitySub );
		} );

		// asset before
		scope.inTransaction( session -> {
			SecondaryTableEntitySub entitySub = session.createQuery(
					"select s from SecondaryTableEntitySub s",
					SecondaryTableEntitySub.class ).getSingleResult();
			assertNotNull( entitySub );
			assertEquals( 111L, entitySub.getB() );
			assertEquals( 222L, entitySub.getC() );
		} );

		// update
		scope.inTransaction( session -> {
			session.createMutationQuery( "update SecondaryTableEntitySub e set e.b=:b, e.c=:c" )
					.setParameter( "b", 333L )
					.setParameter( "c", 444L )
					.executeUpdate();
		} );

		// asset after
		scope.inTransaction( session -> {
			SecondaryTableEntitySub entitySub = session.createQuery( "select s from SecondaryTableEntitySub s",
					SecondaryTableEntitySub.class ).getSingleResult();
			assertNotNull( entitySub );
			assertEquals( 333L, entitySub.getB() );
			assertEquals( 444L, entitySub.getC() );
		} );
	}

	@Entity(name = "SecondaryTableEntitySub")
	@Inheritance(strategy = InheritanceType.JOINED)
	@SecondaryTable(name = "test")
	public static class SecondaryTableEntitySub extends SecondaryTableEntityBase {

		@Column
		private Long b;

		@Column(table = "test")
		private Long c;

		public Long getB() {
			return b;
		}

		public void setB(Long b) {
			this.b = b;
		}

		public Long getC() {
			return c;
		}

		public void setC(Long c) {
			this.c = c;
		}
	}

	@Entity
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class SecondaryTableEntityBase {

		@Id
		@GeneratedValue
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
