package org.hibernate.test.instrument.cases;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.test.common.ServiceRegistryHolder;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractExecutable implements Executable {

	private ServiceRegistryHolder serviceRegistryHolder;
	private SessionFactory factory;

	public final void prepare() {
		Configuration cfg = new Configuration().setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		String[] resources = getResources();
		for ( int i = 0; i < resources.length; i++ ) {
			cfg.addResource( resources[i] );
		}
		serviceRegistryHolder = new ServiceRegistryHolder( cfg.getProperties() );
		factory = cfg.buildSessionFactory( serviceRegistryHolder.getServiceRegistry() );
	}

	public final void complete() {
		try {
			cleanup();
		}
		finally {
			factory.close();
			if ( serviceRegistryHolder != null ) {
				serviceRegistryHolder.destroy();
			}			
		}
	}

	protected SessionFactory getFactory() {
		return factory;
	}

	protected void cleanup() {
	}

	protected String[] getResources() {
		return new String[] { "org/hibernate/test/instrument/domain/Documents.hbm.xml" };
	}
}
