package org.hibernate.test.instrument.cases;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.TestConfigurationHelper;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractExecutable implements Executable {

	private ServiceRegistry serviceRegistry;
	private SessionFactory factory;
    @Override
	public final void prepare() {
		prepare( null );
	}

	@Override
	public final void prepare(ClassLoader instrumentedClassLoader) {
		Configuration cfg = new Configuration().setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder = new BootstrapServiceRegistryBuilder();
		if ( instrumentedClassLoader != null ) // old metamodel case
		{
			bootstrapServiceRegistryBuilder.with( instrumentedClassLoader );
		}
		BootstrapServiceRegistry bootstrapServiceRegistry = bootstrapServiceRegistryBuilder.build();

		StandardServiceRegistryBuilder standardServiceRegistryBuilder = new StandardServiceRegistryBuilder(bootstrapServiceRegistry);
		serviceRegistry = standardServiceRegistryBuilder.applySettings( cfg.getProperties() ).build();

		String[] resources = getResources();
		if( TestConfigurationHelper.DEFAULT_USE_NEW_METAMODEL ){
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
