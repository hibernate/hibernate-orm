package org.hibernate.envers.test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/**
 * Base class for testing envers with Session.
 * 
 * @author Hernán Chanfreau
 *
 */
public abstract class AbstractSessionTest {

	protected Configuration config;
	private SessionFactory sessionFactory;
	private Session session ;
	private AuditReader auditReader;
	
	
	@BeforeClass
    public void init() {
		config = new AnnotationConfiguration();
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource("hibernate.test.session-cfg.xml");
            config.configure(new File(url.toURI()));
            this.initMappings();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		sessionFactory = config.buildSessionFactory();
    }
	
	protected abstract void initMappings() throws MappingException, URISyntaxException ;



	private SessionFactory getSessionFactory(){
		return sessionFactory;
    }


    @BeforeMethod
    public void newSessionFactory() {
      session = getSessionFactory().openSession();
      auditReader = AuditReaderFactory.get(session);
    }
	
	@AfterClass
	public void closeSessionFactory() {
	    sessionFactory.close();
	}
	
	
	protected Session getSession() {
		return session;
	}



	protected AuditReader getAuditReader() {
		return auditReader;
	}

}

