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
import org.testng.annotations.*;

/**
 * Base class for testing envers with Session.
 * 
 * @author Hern�n Chanfreau
 *
 */
public abstract class AbstractSessionTest {

	protected Configuration config;
	private SessionFactory sessionFactory;
	private Session session ;
	private AuditReader auditReader;
	
	
	@BeforeClass
    @Parameters("auditStrategy")
    public void init(@Optional String auditStrategy) throws URISyntaxException {
        config = new AnnotationConfiguration();
        URL url = Thread.currentThread().getContextClassLoader().getResource("hibernate.test.session-cfg.xml");
        config.configure(new File(url.toURI()));

        if (auditStrategy != null && !"".equals(auditStrategy)) {
            config.setProperty("org.hibernate.envers.audit_strategy", auditStrategy);
        }

        this.initMappings();
		
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

