/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;

import static org.junit.Assert.fail;

import java.util.ConcurrentModificationException;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Test;

/**
 * @author Guenther Demetz
 */
public class HibernateAnnotationMappingTest extends BaseUnitTestCase {

	@Test
	@JiraKey( value = "HHH-7446" )
	public void testUniqueConstraintAnnotationOnNaturalIds() throws Exception {
		Configuration configuration = new Configuration();
		ServiceRegistryUtil.applySettings( configuration.getStandardServiceRegistryBuilder() );
		configuration.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		configuration.addAnnotatedClass( Month.class);
		SessionFactory sf = null;
		try {
			sf = configuration.buildSessionFactory();
			sf.close();
		}
		catch (ConcurrentModificationException e) {
			fail(e.toString());
		}
	}
}
