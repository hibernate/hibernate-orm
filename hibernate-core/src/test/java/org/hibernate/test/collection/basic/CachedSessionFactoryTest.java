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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import javax.transaction.TransactionManager;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.SessionFactoryImpl;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaPlatformImpl;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.fail;

public class CachedSessionFactoryTest extends BaseCoreFunctionalTestCase {

	private File cacheFile;

	{
		try {
			cacheFile = File.createTempFile( "sessionFactory", ".ser" );
			cacheFile.deleteOnExit();

		}
		catch (IOException e) {
			fail(e.getMessage());
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { User.class, Contact.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( Environment.HBM2DDL_AUTO, "create" );
		configuration.setProperty( Environment.SESSION_FACTORY_SERIALIZATION_FILE, cacheFile.getAbsolutePath() );
		configuration.setProperty( Environment.SESSION_FACTORY_DEEP_SERIALIZATION, Boolean.TRUE.toString() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10840")
	public void testCachedSessionFactory() throws IOException, ClassNotFoundException {
		doInHibernate(this::sessionFactory, session -> {
			User user = new User();
			user.setName( "john" );
			session.merge( user );
		});

		rebuildSessionFactory();

		doInHibernate(this::sessionFactory, session -> {
			User user = new User();
			user.setName( "john" );
			session.merge( user );
		});
	}

	@Override
	protected boolean rebuildSessionFactoryOnError() {
		return false;
	}

	@Override
	protected boolean failOnServiceRegistryNotClosed() {
		return false;
	}
}
