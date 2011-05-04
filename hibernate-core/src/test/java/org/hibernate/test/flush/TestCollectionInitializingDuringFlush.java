/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.flush;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.EventType;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.integrator.spi.IntegratorService;
import org.hibernate.service.internal.BasicServiceRegistryImpl;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.integrator.spi.Integrator;

import org.junit.Test;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-2763" )
public class TestCollectionInitializingDuringFlush extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Author.class, Book.class, Publisher.class };
	}

	@Override
	protected void applyServices(BasicServiceRegistryImpl serviceRegistry) {
		super.applyServices( serviceRegistry );
		serviceRegistry.getService( IntegratorService.class ).addIntegrator(
				new Integrator() {
					@Override
					public void integrate(
							Configuration configuration,
							SessionFactoryImplementor sessionFactory,
							SessionFactoryServiceRegistry serviceRegistry) {
						serviceRegistry.getService( EventListenerRegistry.class )
								.getEventListenerGroup( EventType.PRE_UPDATE )
								.appendListener( new InitializingPreUpdateEventListener() );
					}

					@Override
					public void disintegrate(
							SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
					}
				}
		);
	}

	@Test
	@FailureExpected( jiraKey = "HHH-2763" )
	public void testInitializationDuringFlush() {
		Session s = openSession();
		s.beginTransaction();
		Publisher publisher = new Publisher( "acme" );
		Author author = new Author( "john" );
		author.setPublisher( publisher );
		publisher.getAuthors().add( author );
		author.getBooks().add( new Book( "Reflections on a Wimpy Kid", author ) );
		s.save( author );
		s.getTransaction().commit();
		s.clear();

		s = openSession();
		s.beginTransaction();
		publisher = (Publisher) s.get( Publisher.class, publisher.getId() );
		publisher.setName( "random nally" );
		s.flush();
		s.getTransaction().commit();
		s.clear();

		s = openSession();
		s.beginTransaction();
		s.delete( author );
		s.getTransaction().commit();
		s.clear();
	}
}
