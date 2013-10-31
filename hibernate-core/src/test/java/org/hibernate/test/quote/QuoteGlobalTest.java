/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.quote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 * @author Brett Meyer
 */
public class QuoteGlobalTest extends BaseCoreFunctionalTestCase {
	
	@Test
	@TestForIssue(jiraKey = "HHH-7890")
	public void testQuotedUniqueConstraint() {
		Iterator<UniqueKey> itr = configuration().getClassMapping( Person.class.getName() )
				.getTable().getUniqueKeyIterator();
		while ( itr.hasNext() ) {
			UniqueKey uk = itr.next();
			assertEquals( uk.getColumns().size(), 1 );
			assertEquals( uk.getColumn( 0 ).getName(),  "name");
			return;
		}
		fail( "GLOBALLY_QUOTED_IDENTIFIERS caused the unique key creation to fail." );
	}
	
	@Test
	public void testQuoteManytoMany() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		User u = new User();
		s.persist( u );
		Role r = new Role();
		s.persist( r );
		u.getRoles().add( r );
		s.flush();
		s.clear();
		u = (User) s.get( User.class, u.getId() );
		assertEquals( 1, u.getRoles().size() );
		tx.rollback();
		String role = User.class.getName() + ".roles";
		assertEquals( "User_Role", configuration().getCollectionMapping( role ).getCollectionTable().getName() );
		s.close();
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-8520")
	public void testHbmQuoting() {
		doTestHbmQuoting( DataPoint.class );
		doTestHbmQuoting( AssociatedDataPoint.class );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7927")
	public void testTableGeneratorQuoting() {
		Configuration configuration = constructAndConfigureConfiguration();
		configuration.addAnnotatedClass(TestEntity.class);
		new SchemaExport(configuration).execute( false, true, false, true );
		try {
			new SchemaValidator(configuration).validate();
		}
		catch (HibernateException e) {
			fail( "The identifier generator table should have validated.  " + e.getMessage() );
		}
	}
	
	private void doTestHbmQuoting(Class clazz) {
		Table table = configuration().getClassMapping( clazz.getName() ).getTable();
		assertTrue( table.isQuoted() );
		Iterator itr = table.getColumnIterator();
		while(itr.hasNext()) {
			Column column = (Column) itr.next();
			assertTrue( column.isQuoted() );
		}
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.GLOBALLY_QUOTED_IDENTIFIERS, "true" );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				User.class,
				Role.class,
				Phone.class,
				Person.class,
				House.class
		};
	}

	@Override
	protected String[] getMappings() {
		return new String[] { "quote/DataPoint.hbm.xml" };
	}
	
	@Entity
	private static class TestEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.TABLE)
		private int id;
	}
}
