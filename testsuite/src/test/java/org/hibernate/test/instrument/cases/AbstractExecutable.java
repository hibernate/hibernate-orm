package org.hibernate.test.instrument.cases;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractExecutable implements Executable {

	private SessionFactory factory;

	public final void prepare() {
		Configuration cfg = new Configuration().setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		String[] resources = getResources();
		for ( int i = 0; i < resources.length; i++ ) {
			cfg.addResource( resources[i] );
		}
		factory = cfg.buildSessionFactory();
	}

	public final void complete() {
		try {
			cleanup();
		}
		finally {
			factory.close();
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
