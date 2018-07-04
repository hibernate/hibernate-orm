/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.insertordering;

import java.sql.SQLException;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;

import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12407")
public class InsertOrderingWithBaseClassReferencingSubclass
		extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				OwningTable.class,
				TableB.class,
				TableA.class,
				LinkTable.class
		};
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( Environment.ORDER_INSERTS, "true" );
		settings.put( Environment.STATEMENT_BATCH_SIZE, "10" );
	}

	@Test
	public void testBatching() throws SQLException {
		doInHibernate( this::sessionFactory, session -> {
			OwningTable rec_owningTable = new OwningTable();
			session.persist(rec_owningTable);

			session.flush();

			TableB rec_tableB = new TableB();
			rec_tableB.owning = rec_owningTable;
			session.persist(rec_tableB);

			TableA rec_tableA = new TableA();
			rec_tableA.owning = rec_owningTable;
			session.persist(rec_tableA);

			LinkTable rec_link = new LinkTable();
			rec_link.refToA = rec_tableA;
			rec_link.refToB = rec_tableB;

			session.persist(rec_link);
		} );

	}

	@Entity(name = "RootTable")
	@Inheritance(strategy = InheritanceType.JOINED)
	public abstract static class RootTable {
		@Id
		@GeneratedValue
		public int sysId;
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

		@ManyToOne
		public TableA refToA;

		@ManyToOne
		public TableB refToB;
	}
}
