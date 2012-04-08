package org.hibernate.envers.test;

import java.net.URISyntaxException;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.envers.test.entities.reventity.OracleRevisionEntity;
import org.hibernate.envers.test.entities.reventity.trackmodifiedentities.OracleTrackingModifiedEntitiesRevisionEntity;
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
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public abstract class AbstractSessionTest extends AbstractEnversTest {
    public static final Dialect DIALECT = Dialect.getDialect();

	protected Configuration config;
	private ServiceRegistry serviceRegistry;
	private SessionFactory sessionFactory;
	private Session session ;
	private AuditReader auditReader;

    protected static Dialect getDialect() {
        return DIALECT;
    }

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
        revisionEntityForDialect(config, getDialect());

		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( config.getProperties() );
		sessionFactory = config.buildSessionFactory( serviceRegistry );
    }
	protected boolean createSchema() {
		return true;
	}
	protected abstract void initMappings() throws MappingException, URISyntaxException ;

    protected void revisionEntityForDialect(Configuration cfg, Dialect dialect) {
        if (dialect instanceof Oracle8iDialect) {
            if (Boolean.parseBoolean(config.getProperty("org.hibernate.envers.track_entities_changed_in_revision"))) {
                cfg.addAnnotatedClass(OracleTrackingModifiedEntitiesRevisionEntity.class);
            } else {
                cfg.addAnnotatedClass(OracleRevisionEntity.class);
            }
        }
    }

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

