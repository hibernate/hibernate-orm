/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;

import org.hibernate.cfg.Environment;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-12407")
@SessionFactory
@DomainModel(
		annotatedClasses = {
				InsertOrderingWithBaseClassReferencingSubclass.OwningTable.class,
				InsertOrderingWithBaseClassReferencingSubclass.TableB.class,
				InsertOrderingWithBaseClassReferencingSubclass.TableA.class,
				InsertOrderingWithBaseClassReferencingSubclass.LinkTable.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.ORDER_INSERTS, value = "true"),
				@Setting(name = Environment.STATEMENT_BATCH_SIZE, value = "10")
		}
)
public class InsertOrderingWithBaseClassReferencingSubclass {

	@Test
	public void testBatching(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			OwningTable rec_owningTable = new OwningTable();
			session.persist( rec_owningTable );

			session.flush();

			TableB rec_tableB = new TableB();
			rec_tableB.owning = rec_owningTable;
			session.persist( rec_tableB );

			TableA rec_tableA = new TableA();
			rec_tableA.owning = rec_owningTable;
			session.persist( rec_tableA );

			LinkTable rec_link = new LinkTable();
			rec_link.refToA = rec_tableA;
			rec_link.refToB = rec_tableB;

			session.persist( rec_link );
		} );

	}

	@Entity(name = "RootTable")
	@Inheritance(strategy = InheritanceType.JOINED)
	public abstract static class RootTable {
		@Id
		@GeneratedValue
		public int sysId;

		public String name;
	}

	@Entity(name = "OwnedTable")
	public abstract static class OwnedTable extends RootTable {
		@ManyToOne
		public OwningTable owning;
	}

	@Entity(name = "OwningTable")
	public static class OwningTable extends OwnedTable {
	}

	@Entity(name = "TableA")
	public static class TableA extends OwnedTable {
	}

	@Entity(name = "TableB")
	public static class TableB extends OwnedTable {
	}

	@Entity(name = "LinkTable")
	public static class LinkTable {
		@Id
		@GeneratedValue
		public int sysId;

		public String name;

		@ManyToOne
		public TableA refToA;

		@ManyToOne
		public TableB refToB;
	}
}
