package org.hibernate.envers.test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.Before;

import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.AfterClassOnce;
import org.hibernate.testing.BeforeClassOnce;
import org.hibernate.testing.ServiceRegistryBuilder;

/**
 * Base class for testing envers with Session.
 *
 * @author Hern&aacute;n Chanfreau
 *
 */
public abstract class AbstractSessionTest extends AbstractEnversTest {

	protected Configuration config;
	private ServiceRegistry serviceRegistry;
	private SessionFactory sessionFactory;
	private Session session ;
	private AuditReader auditReader;


	@BeforeClassOnce
    public void init() throws URISyntaxException {
        config = new Configuration();
		if ( createSchema() ) {
			config.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		}
        String auditStrategy = getAuditStrategy();
        if (auditStrategy != null && !"".equals(auditStrategy)) {
            config.setProperty("org.hibernate.envers.audit_strategy", auditStrategy);
        }

        this.initMappings();

		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( config.getProperties() );
		sessionFactory = config.buildSessionFactory( serviceRegistry );
    }
	protected boolean createSchema() {
		return true;
	}
	protected abstract void initMappings() throws MappingException, URISyntaxException ;

	private SessionFactory getSessionFactory(){
		return sessionFactory;
    }

    @Before
    public void newSessionFactory() {
      session = getSessionFactory().openSession();
      auditReader = AuditReaderFactory.get(session);
    }

	@AfterClassOnce
	public void closeSessionFactory() {
		try {
	   		sessionFactory.close();
		}
		finally {
			if ( serviceRegistry != null ) {
				ServiceRegistryBuilder.destroy( serviceRegistry );
				serviceRegistry = null;
			}
		}
	}


	protected Session getSession() {
		return session;
	}

	protected Configuration getCfg() {
		return config;
	}

	protected AuditReader getAuditReader() {
		return auditReader;
	}

}

