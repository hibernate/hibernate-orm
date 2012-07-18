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
package org.hibernate.jpa.test.lob;

import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;

import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
@RequiresDialectFeature( DialectChecks.SupportsExpectedLobUsagePattern.class )
public class BlobTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testBlobSerialization() throws Exception {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Map<String,String> image = new HashMap<String, String>();
		image.put( "meta", "metadata" );
		image.put( "data", "imagedata" );
		ImageReader reader = new ImageReader();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream( baos );
		oos.writeObject( image );
		reader.setImage( em.unwrap( Session.class ).getLobHelper().createBlob( baos.toByteArray() ) );
		em.persist( reader );
		em.getTransaction().commit();
		em.close(); //useless but y'a know
		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		reader = em.find( ImageReader.class, reader.getId() );
		ObjectInputStream ois = new ObjectInputStream( reader.getImage().getBinaryStream() );
		image = (HashMap<String, String>) ois.readObject();
		assertTrue( image.containsKey( "meta" ) );
		em.getTransaction().commit();
		em.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] { ImageReader.class };
	}
}
