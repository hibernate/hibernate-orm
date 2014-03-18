/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.osgi.test.client;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;


/**
 * @author Brett Meyer
 */
public class TestServiceImpl implements TestService {
	
	private final EntityManagerFactory emf;
	
	private final SessionFactory sf;
	
	private final TestIntegrator testIntegrator;
	
	private final TestStrategyRegistrationProvider testStrategyRegistrationProvider;
	
	private final TestTypeContributor testTypeContributor;
	
	public TestServiceImpl(BundleContext context, TestIntegrator testIntegrator,
			TestStrategyRegistrationProvider testStrategyRegistrationProvider, TestTypeContributor testTypeContributor) {
		final ServiceReference serviceReference = context.getServiceReference( PersistenceProvider.class.getName() );
		final PersistenceProvider persistenceProvider = (PersistenceProvider) context.getService( serviceReference );
		emf = persistenceProvider.createEntityManagerFactory( "hibernate-osgi-test", null );
		
		final ServiceReference sr = context.getServiceReference( SessionFactory.class.getName() );
		sf = (SessionFactory) context.getService( sr );

		this.testIntegrator = testIntegrator;
		this.testStrategyRegistrationProvider = testStrategyRegistrationProvider;
		this.testTypeContributor = testTypeContributor;
	}
	
	public void saveJpa(DataPoint dp) {
		final EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.persist( dp );
		em.getTransaction().commit();
		em.close();
	}
	
	public DataPoint getJpa(long id) {
		final EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		DataPoint dp = em.find(DataPoint.class, id);
		em.getTransaction().commit();
		em.close();
		return dp;
	}
	
	public void updateJpa(DataPoint dp) {
		final EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.merge( dp );
		em.getTransaction().commit();
		em.close();
	}
	
	public void deleteJpa() {
		final EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete from DataPoint" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}
	
	public void saveNative(DataPoint dp) {
		final Session s = sf.openSession();
		s.getTransaction().begin();
		s.persist( dp );
		s.getTransaction().commit();
		s.close();
	}
	
	public DataPoint getNative(long id) {
		final Session s = sf.openSession();
		s.getTransaction().begin();
		DataPoint dp = (DataPoint) s.get( DataPoint.class, id );
		s.getTransaction().commit();
		s.close();
		return dp;
	}
	
	public void updateNative(DataPoint dp) {
		final Session s = sf.openSession();
		s.getTransaction().begin();
		s.update( dp );
		s.getTransaction().commit();
		s.close();
	}
	
	public void deleteNative() {
		final Session s = sf.openSession();
		s.getTransaction().begin();
		s.createQuery( "delete from DataPoint" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}
	
	public DataPoint lazyLoad(long id) {
		final Session s = sf.openSession();
		s.getTransaction().begin();
		final DataPoint dp = (DataPoint) s.load( DataPoint.class, new Long( id ) );
		Hibernate.initialize( dp );
		s.getTransaction().commit();
		s.close();
		return dp;
	}
	
	public TestIntegrator getTestIntegrator() {
		return testIntegrator;
	}
	
	public TestStrategyRegistrationProvider getTestStrategyRegistrationProvider() {
		return testStrategyRegistrationProvider;
	}
	
	public TestTypeContributor getTestTypeContributor() {
		return testTypeContributor;
	}
}
