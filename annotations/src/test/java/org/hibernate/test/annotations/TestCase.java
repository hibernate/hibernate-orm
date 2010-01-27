//$Id$
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations;

import java.io.InputStream;

import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * A base class for all annotation tests.
 *
 * @author Emmnauel Bernand
 * @author Hardy Ferentschik
 */
public abstract class TestCase extends HibernateTestCase {

	protected static SessionFactory sessions;
	private Session session;

	public TestCase() {
		super();
	}

	public TestCase(String name) {
		super( name );
	}

	public Session openSession() throws HibernateException {
		rebuildSessionFactory();
		session = getSessions().openSession();
		return session;
	}

	public Session openSession(Interceptor interceptor) throws HibernateException {
		rebuildSessionFactory();
		session = getSessions().openSession( interceptor );
		return session;
	}

	private void rebuildSessionFactory() {
		if ( sessions == null ) {
			try {
				buildConfiguration();
			}
			catch ( Exception e ) {
				throw new HibernateException( e );
			}
		}
	}	

	protected void setSessions(SessionFactory sessions) {
		TestCase.sessions = sessions;
	}

	protected SessionFactory getSessions() {
		return sessions;
	}

	protected SessionFactoryImplementor sfi() {
		return (SessionFactoryImplementor) getSessions();
	}

	@Override	
	protected void buildConfiguration() throws Exception {
		if ( getSessions() != null ) {
			getSessions().close();
		}
		try {
			setCfg( new AnnotationConfiguration() );
			configure( cfg );
			if ( recreateSchema() ) {
				cfg.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
			}
			for ( String aPackage : getAnnotatedPackages() ) {
				( ( AnnotationConfiguration ) getCfg() ).addPackage( aPackage );
			}
			for ( Class<?> aClass : getAnnotatedClasses() ) {
				( ( AnnotationConfiguration ) getCfg() ).addAnnotatedClass( aClass );
			}
			for ( String xmlFile : getXmlFiles() ) {
				InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( xmlFile );
				getCfg().addInputStream( is );
			}
			setSessions( getCfg().buildSessionFactory( /* new TestInterceptor() */ ) );
		}
		catch ( Exception e ) {
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	protected void handleUnclosedResources() {
		if ( session != null && session.isOpen() ) {
			if ( session.isConnected() ) {
				session.doWork( new RollbackWork() );
			}
			session.close();
			session = null;
			fail( "unclosed session" );
		}
		else {
			session = null;
		}
	}

	@Override
	protected void closeResources() {
		try {
			if ( session != null && session.isOpen() ) {
				if ( session.isConnected() ) {
					session.doWork( new RollbackWork() );
				}
				session.close();
			}
		}
		catch ( Exception ignore ) {
		}
		try {
			if ( sessions != null ) {
				sessions.close();
				sessions = null;
			}
		}
		catch ( Exception ignore ) {
		}
	}
}
