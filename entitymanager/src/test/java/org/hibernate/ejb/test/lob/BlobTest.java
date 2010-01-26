//$Id$
package org.hibernate.ejb.test.lob;

import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Blob;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;

import org.hibernate.Hibernate;
import org.hibernate.dialect.Dialect;
import org.hibernate.ejb.test.TestCase;

/**
 * @author Emmanuel Bernard
 */
public class BlobTest extends TestCase {

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
		reader.setImage( (Blob) Hibernate.createBlob( baos.toByteArray() ) );
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
	public boolean appliesTo(Dialect dialect) {
		return dialect.supportsExpectedLobUsagePattern();
	}

	public Class[] getAnnotatedClasses() {
		return new Class[]{
				ImageReader.class
		};
	}
}
