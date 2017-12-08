/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit5.envers;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.jboss.logging.Logger;

import org.hibernate.testing.junit5.SessionFactoryAccess;

/**
 * A scope or holder for the SessionFactory instance associated with a given test class.
 * Used to:
 * <ul>
 *     <li>Provide lifecycle management related to the SessionFactory</li>
 * </ul>
 *
 * @author Chris Cranford
 */
public class EnversSessionFactoryScope implements SessionFactoryAccess {
	private static final Logger log = Logger.getLogger( EnversSessionFactoryScope.class );

	private final EnversSessionFactoryProducer sessionFactoryProducer;
	private final Strategy auditStrategy;

	private SessionFactory sessionFactory;

	public EnversSessionFactoryScope(EnversSessionFactoryProducer producer, Strategy auditStrategy) {
		log.debugf( "#<init> - %s", auditStrategy.getDisplayName() );
		this.auditStrategy = auditStrategy;
		this.sessionFactoryProducer = producer;
	}

	public void releaseSessionFactory() {
		log.debugf( "#releaseSessionFactory - %s", auditStrategy.getDisplayName() );
		if ( sessionFactory != null ) {
			log.infof( "Closing SessionFactory %s (%s)", sessionFactory, auditStrategy.getDisplayName() );
			sessionFactory.close();
			sessionFactory = null;
		}
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		log.debugf( "#getSessionFactory - %s", auditStrategy.getDisplayName() );
		if ( sessionFactory == null || sessionFactory.isClosed() ) {
			sessionFactory = sessionFactoryProducer.produceSessionFactory( auditStrategy.getSettingValue() );
		}
		return (SessionFactoryImplementor) sessionFactory;
	}

}
