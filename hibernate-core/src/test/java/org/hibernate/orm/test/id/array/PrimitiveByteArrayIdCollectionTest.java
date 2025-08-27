/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.array;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SkipForDialect(dialectClass = MySQLDialect.class, majorVersion = 5, reason = "BLOB/TEXT column 'id' used in key specification without a key length")
@SkipForDialect(dialectClass = OracleDialect.class, reason = "ORA-02329: column of datatype LOB cannot be unique or a primary key")
@SkipForDialect(dialectClass = InformixDialect.class, reason = "Informix does not support unique / primary constraints on binary columns")
@DomainModel(
		annotatedClasses = {
				PrimitiveByteArrayIdCollectionTest.Parent.class,
				PrimitiveByteArrayIdCollectionTest.Child.class
		}
)
@SessionFactory
public class PrimitiveByteArrayIdCollectionTest {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent entity = new Parent();
					entity.id = new byte[] {
							(byte) ( 1 ),
							(byte) ( 2 ),
							(byte) ( 3 ),
							(byte) ( 4 )
					};
					entity.name = "Simple name";

					for ( int j = 1; j <= 2; j++ ) {
						Child child = new Child();
						child.id = j;
						entity.name = "Child name " + j;
						child.parent = entity;
						entity.children.add(child);
					}
					session.persist( entity );
				}
		);
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-7180")
	public void testReattach(SessionFactoryScope scope) {
		// Since reattachment was removed in ORM 7,
		// but the code path to trigger the bug is still reachable through removing a detached entity,
		// construct a scenario that shows a problem

		final Parent parent = scope.fromTransaction(
				session -> session.createQuery( "from Parent p", Parent.class ).getSingleResult()
		);
		// Copy the byte-array id which will make it a different instance, yet equal to the collection key
		parent.id = parent.id.clone();

		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.setStatisticsEnabled( true );
		statistics.clear();

		scope.inTransaction(
				session -> {
					session.remove( parent );
					session.flush();

					// The collection will be removed twice if the collection key can't be matched to the entity id
					assertEquals( 1L, statistics.getCollectionRemoveCount() );
				}
		);

	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		public byte[] id;
		public String name;
		@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
		public Set<Child> children = new HashSet<>();
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		public Integer id;
		public String name;
		@ManyToOne(fetch = FetchType.LAZY)
		public Parent parent;

		public Child() {
		}

		public Child(Integer id) {
			this.id = id;
		}
	}
}
