/*
 * Copyright (C) 2016 CNH Industrial NV. All rights reserved.
 *
 * This software contains proprietary information of CNH Industrial NV. Neither
 * receipt nor possession thereof confers any right to reproduce, use, or
 * disclose in whole or in part any such information without written
 * authorization from CNH Industrial NV.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.basic;

import javax.transaction.TransactionManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.internal.SessionFactoryImpl.HIBERNATE_DEEPSERIALIZE;

public class CachedSessionFactoryTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {User.class,Contact.class};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure(configuration);
		configuration.setProperty( Environment.HBM2DDL_AUTO, "create" );
	}

	@Override
	protected void prepareTest() throws Exception {
		File cacheFile = new File("sf.bin");
		if(cacheFile.exists()) cacheFile.delete();
	}

	public static class TM_placeholder implements Serializable {}

	@Test
	@TestForIssue(jiraKey = "HHH-10840")
	public void testCachedSessionFactory() throws IOException, ClassNotFoundException {
		Session session = openSession();
		session.beginTransaction();

		User user = new User();
		user.setName( "john" );
		user = (User) session.merge( user );

		session.getTransaction().commit();
		session.close();

		System.setProperty( HIBERNATE_DEEPSERIALIZE, "true" );
		File cacheFile = new File("sf.bin");

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(cacheFile);
			ObjectOutputStream oos = new ObjectOutputStream(fos) {
				{
					enableReplaceObject(true);
				}

				@Override
				protected Object replaceObject(Object obj) throws IOException {
					if(obj instanceof TransactionManager) {
						return new TM_placeholder();
					}
					return super.replaceObject(obj);
				}
			};
			oos.writeObject(sessionFactory());
		} finally {
			fos.close();
		}
		Assert.assertTrue("Cache file exists", cacheFile.exists());
		//Force recreation of the session factory
		sessionFactory().close();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(cacheFile);
			ObjectInputStream ois = new ObjectInputStream(fis) {
				{
					enableResolveObject(true);
				}

				@Override
				protected Object resolveObject(Object obj) throws IOException {
					if (obj instanceof TM_placeholder) {
						return TestingJtaPlatformImpl.INSTANCE.getTransactionManager();
					}
					return super.resolveObject(obj);
				}
			};
			sessionFactory = (SessionFactoryImpl) ois.readObject();
		} finally {
			fis.close();
		}
//		buildSessionFactory();

		session = openSession();
		session.beginTransaction();

		user = new User();
		user.setName( "john serialized" );
		user = (User) session.merge( user );

		session.getTransaction().commit();
		session.close();
	}

}
