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
package org.hibernate.test.annotations.inheritance.mixed;

import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emmanuel Bernard
 */
public class SubclassTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testDefault() throws Exception {
		Session s;
		Transaction tx;
		s = openSession();
		tx = s.beginTransaction();
		File doc = new Document( "Enron Stuff To Shred", 1000 );
		Folder folder = new Folder( "Enron" );
		s.persist( doc );
		s.persist( folder );
		try {
			tx.commit();
		}
		catch (SQLGrammarException e) {
			System.err.println( e.getSQLException().getNextException() );
		}
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		List result = s.createCriteria( File.class ).list();
		assertNotNull( result );
		assertEquals( 2, result.size() );
		File f2 = (File) result.get( 0 );
		checkClassType( f2, doc, folder );
		f2 = (File) result.get( 1 );
		checkClassType( f2, doc, folder );
		s.delete( result.get( 0 ) );
		s.delete( result.get( 1 ) );
		tx.commit();
		s.close();
	}

	private void checkClassType(File fruitToTest, File f, Folder a) {
		if ( fruitToTest.getName().equals( f.getName() ) ) {
			assertFalse( fruitToTest instanceof Folder );
		}
		else if ( fruitToTest.getName().equals( a.getName() ) ) {
			assertTrue( fruitToTest instanceof Folder );
		}
		else {
			fail( "Result does not contains the previously inserted elements" );
		}
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				File.class,
				Folder.class,
				Document.class,
				SymbolicLink.class
		};
	}

}
