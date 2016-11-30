/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.proxy.narrow;

import java.util.List;

import junit.framework.Assert;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.Version;
import org.hibernate.test.proxy.narrow.AdvancedUser;
import org.hibernate.test.proxy.narrow.AdvancedUserDetail;
import org.hibernate.test.proxy.narrow.User;
import org.hibernate.test.proxy.narrow.UserConfig;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Yoann Rodi��re
 * @author Guillaume Smet
 */
public class ProxyNarrowingTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { AbstractEntity.class, ConcreteEntity.class, LazyAbstractEntityReference.class };
	}
	
	@Override
	protected String[] getMappings() {
		return new String[] { "Models.hbm.xml", };
	}

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/test/proxy/narrow/";
	}	

	@Test
	public void testNarrowedProxyIsInitializedIfOriginalProxyIsInitialized() {
		Session session = openSession();

		Integer entityReferenceId = null;

		// Populate the database
		try {
			Transaction t = session.beginTransaction();

			ConcreteEntity entity = new ConcreteEntity();
			session.save( entity );

			LazyAbstractEntityReference reference = new LazyAbstractEntityReference( entity );
			session.save( reference );
			entityReferenceId = reference.getId();

			session.flush();
			t.commit();
		}
		finally {
			session.close();
		}

		session = openSession();

		try {
			session.beginTransaction();

			// load a proxified version of the entity into the session: the proxy is based on the AbstractEntity class
			// as the reference class property is of type AbstractEntity.
			LazyAbstractEntityReference reference = session.get( LazyAbstractEntityReference.class, entityReferenceId );
			AbstractEntity abstractEntityProxy = reference.getEntity();

			assertTrue( ( abstractEntityProxy instanceof HibernateProxy ) && !Hibernate.isInitialized( abstractEntityProxy ) );
			Hibernate.initialize( abstractEntityProxy );
			assertTrue( Hibernate.isInitialized( abstractEntityProxy ) );

			// load the concrete class via session.load to trigger the StatefulPersistenceContext.narrowProxy code
			ConcreteEntity concreteEntityProxy = session.load( ConcreteEntity.class, abstractEntityProxy.getId() );

			// the new proxy created should be initialized
			assertTrue( Hibernate.isInitialized( concreteEntityProxy ) );
			assertTrue( session.contains( concreteEntityProxy ) );


			// clean up
			session.delete( reference );
			session.delete( concreteEntityProxy );

			session.getTransaction().commit();
		}
		finally {
			session.close();
		}
	}

	@Test
	public void testHHH11280() {
		Session s = openSession();
		try {
			// Populate the database
			{
				s.beginTransaction();
				UserConfig config = new UserConfig();
				AdvancedUserDetail detail = new AdvancedUserDetail();
				AdvancedUser advUser = new AdvancedUser();
				detail.setId(9L);
				detail.setStatusCode("A");
				detail.setAdvancedUser(advUser);
				advUser.setId(11L);
				advUser.setType(106);
				advUser.setCurrentDetail(detail);
				config.setId(10L);
				config.setUser(advUser);
				s.persist(advUser);
				s.persist(detail);
				s.persist(config);
				s.flush();
				s.getTransaction().commit();
			}

			// Get on with the test.
			s = openSession();
			DetachedCriteria configCriteria = DetachedCriteria.forClass(UserConfig.class);
			configCriteria.add(Restrictions.eq("id", new Long(10)));
			final List<UserConfig> configRows = configCriteria.getExecutableCriteria(s).list();
			UserConfig config = configRows.get(0);
			User configUser = config.getUser();
			Assert.assertNotNull(configUser);
	
			try {
				DetachedCriteria userCriteria = DetachedCriteria.forClass(User.class);
				userCriteria.add(Restrictions.eq("id", new Long(11)));
				final List<User> userRows = userCriteria.getExecutableCriteria(s).list();
				User user = userRows.get(0);
				Assert.assertEquals(106, user.getType().intValue());
				AdvancedUser advancedUser = (AdvancedUser)user;
				Assert.assertNotNull(advancedUser);
			}
			catch(ClassCastException e) {
				Assert.fail(String.format("The User proxy is not an AdvancedUser instance but should be (HHH-11280). Hibernate version detected as '%s'. %s", Version.getVersionString(), e.getMessage()));
			}
		}
		finally {
			s.close();
		}
	}
}
