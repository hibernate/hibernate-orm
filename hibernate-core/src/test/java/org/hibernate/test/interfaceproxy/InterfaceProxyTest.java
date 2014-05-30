/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.interfaceproxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;


/**
 * @author Gavin King
 */
public class InterfaceProxyTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "interfaceproxy/Item.hbm.xml" };
	}

	@Override
	public String getCacheConcurrencyStrategy() {
		return null;
	}

	@Test
	@RequiresDialectFeature(
			value = DialectChecks.SupportsExpectedLobUsagePattern.class,
			comment = "database/driver does not support expected LOB usage pattern"
	)
	public void testInterfaceProxies() {
		Session s = openSession( new DocumentInterceptor() );
		Transaction t = s.beginTransaction();
		Document d = new DocumentImpl();
		d.setName("Hibernate in Action");
		d.setContent( s.getLobHelper().createBlob( "blah blah blah".getBytes() ) );
		Long did = (Long) s.save(d);
		SecureDocument d2 = new SecureDocumentImpl();
		d2.setName("Secret");
		d2.setContent( s.getLobHelper().createBlob( "wxyz wxyz".getBytes() ) );
		// SybaseASE15Dialect only allows 7-bits in a byte to be inserted into a tinyint 
		// column (0 <= val < 128)		
		d2.setPermissionBits( (byte) 127 );
		d2.setOwner("gavin");
		Long d2id = (Long) s.save(d2);
		t.commit();
		s.close();

		s = openSession( new DocumentInterceptor() );
		t = s.beginTransaction();
		d = (Document) s.load(ItemImpl.class, did);
		assertEquals( did, d.getId() );
		assertEquals( "Hibernate in Action", d.getName() );
		assertNotNull( d.getContent() );
		
		d2 = (SecureDocument) s.load(ItemImpl.class, d2id);
		assertEquals( d2id, d2.getId() );
		assertEquals( "Secret", d2.getName() );
		assertNotNull( d2.getContent() );
		
		s.clear();
		
		d = (Document) s.load(DocumentImpl.class, did);
		assertEquals( did, d.getId() );
		assertEquals( "Hibernate in Action", d.getName() );
		assertNotNull( d.getContent() );
		
		d2 = (SecureDocument) s.load(SecureDocumentImpl.class, d2id);
		assertEquals( d2id, d2.getId() );
		assertEquals( "Secret", d2.getName() );
		assertNotNull( d2.getContent() );
		assertEquals( "gavin", d2.getOwner() );
		
		//s.clear();
		
		d2 = (SecureDocument) s.load(SecureDocumentImpl.class, did);
		assertEquals( did, d2.getId() );
		assertEquals( "Hibernate in Action", d2.getName() );
		assertNotNull( d2.getContent() );
		
		try {
			d2.getOwner(); //CCE
			assertFalse(true);
		}
		catch (ClassCastException cce) {
			//correct
		}

		s.createQuery( "delete ItemImpl" ).executeUpdate();
		t.commit();
		s.close();
	}
}

