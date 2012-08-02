//$Id$
package org.hibernate.ejb.test.ejb3configuration;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.test.Cat;
import org.hibernate.ejb.test.Kitten;
import org.hibernate.internal.util.ConfigHelper;
import org.hibernate.testing.RequiresDialect;

/**
 * @author Emmanuel Bernard
 */
@RequiresDialect(H2Dialect.class)
public class ProgrammaticConfTest {
    @Test
	public void testProgrammaticAPI() throws Exception {
		Ejb3Configuration conf = new Ejb3Configuration();
		conf.addAnnotatedClass( Cat.class );
		conf.addAnnotatedClass( Kitten.class );
		conf.addProperties(getProperties());
		EntityManagerFactory emf = conf.buildEntityManagerFactory();
		EntityManager em = emf.createEntityManager();
		Cat cat = new Cat();
		cat.setAge( 23 );
		cat.setDateOfBirth( new Date() );
		cat.setLength( 32 );
		cat.setName( "Tomy" );
		em.getTransaction().begin();
		em.persist( cat );
		em.flush();
		Assert.assertNotNull( em.find( Cat.class, cat.getId() ) );
		em.getTransaction().rollback();
		emf.close();
	}
    @Test
	public void testProgrammaticCfg() throws Exception {
		Ejb3Configuration conf = new Ejb3Configuration();
		conf.configure( "org/hibernate/ejb/test/ejb3configuration/hibernate.cfg.xml" );
		conf.addProperties(getProperties());
		EntityManagerFactory emf = conf.buildEntityManagerFactory();
		EntityManager em = emf.createEntityManager();
		Cat cat = new Cat();
		cat.setAge( 23 );
		cat.setDateOfBirth( new Date() );
		cat.setLength( 32 );
		cat.setName( "Tomy" );
		em.getTransaction().begin();
		em.persist( cat );
		em.flush();
		Assert.assertNotNull( em.find(Cat.class, cat.getId() ) );
		em.getTransaction().rollback();
		emf.close();
	}

	protected Properties getProperties() throws IOException {
		Properties properties = new Properties( );
		InputStream stream = ConfigHelper.getResourceAsStream("/hibernate.properties");
		try {
			properties.load(stream);
		}

		finally {
			try{
				stream.close();
			}
			catch (IOException ioe){
			}
		}
		properties.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		return properties;
	}


}
