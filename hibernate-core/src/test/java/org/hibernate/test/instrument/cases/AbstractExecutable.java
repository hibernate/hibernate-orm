package org.hibernate.test.instrument.cases;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractExecutable implements Executable {

	private ServiceRegistry serviceRegistry;
	private SessionFactory factory;
    @Override
	public final void prepare() {
		Configuration cfg = new Configuration().setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( cfg.getProperties() );
		String[] resources = getResources();
		if( BaseUnitTestCase.isMetadataUsed()){
			MetadataSources metadataSources = new MetadataSources( serviceRegistry );
			for(String resource : resources){
				metadataSources.addResource( resource );
			}
			factory = metadataSources.buildMetadata().buildSessionFactory();
		}else{
			for ( String resource : resources ) {
				cfg.addResource( resource );
			}
			factory = cfg.buildSessionFactory( serviceRegistry );
		}

	}
    @Override
	public final void complete() {
		try {
			cleanup();
		}
		finally {
			factory.close();
			factory = null;
			if ( serviceRegistry != null ) {
				ServiceRegistryBuilder.destroy( serviceRegistry );
				serviceRegistry = null;
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
