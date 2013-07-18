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
package org.hibernate.test.annotations.quote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.UniqueKey;
import org.hibernate.test.util.SchemaUtil;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 * @author Brett Meyer
 */
public class QuoteGlobalTest extends BaseCoreFunctionalTestCase {
	
	@Test
	@TestForIssue(jiraKey = "HHH-7890")
	public void testQuotedUniqueConstraint() {
		TableSpecification table = SchemaUtil.getTable( Person.class, metadata() );
		Iterator<UniqueKey> itr = table.getUniqueKeys().iterator();
		while ( itr.hasNext() ) {
			UniqueKey uk = itr.next();
			assertEquals( uk.getColumns().size(), 1 );
			assertEquals( uk.getColumns().get( 0 ).getColumnName().getText(),  "name");
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
		assertEquals( "`User_Role`", SchemaUtil.getCollectionTable( User.class, "roles", metadata() ).getLogicalName()
				.toString() );
		s.close();
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.GLOBALLY_QUOTED_IDENTIFIERS, "true" );
	}
	
	@Override
	protected void prepareStandardServiceRegistryBuilder(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( Environment.GLOBALLY_QUOTED_IDENTIFIERS, "true" );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				User.class,
				Role.class,
				Phone.class,
				Person.class
		};
	}
}
