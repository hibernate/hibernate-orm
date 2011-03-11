/* 
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.test.dialect.detection;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.dialect.function.Product;
import org.hibernate.testing.junit.functional.FunctionalTestCase;

/**
 * Verifies the Dialect is detected even if some non critical metadata
 * is not provided by the JDBC driver.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class DialectDetectWithoutMetadataTest extends FunctionalTestCase {
	
	public DialectDetectWithoutMetadataTest( String string ) {
		super( string );
	}

	public String[] getMappings() {
		return new String[] { "dialect/function/Product.hbm.xml" };
	}
	
	public void testBasicOperations() {
		// save and query a single entity:
		// enough to know the configuration is working.
		Product product = new Product();
		product.setLength( 100 );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.save( product );
		tx.commit();
		s.clear();
		
		tx = s.beginTransaction();
		Query q = s.createQuery( "from Product p" );
		q.uniqueResult();
		tx.commit();
		s.close();
	}

}
