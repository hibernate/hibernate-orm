/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import java.util.ConcurrentModificationException;

import static org.assertj.core.api.Fail.fail;

/**
 * @author Guenther Demetz
 */
@BaseUnitTest
public class HibernateAnnotationMappingTest {

	@Test
	@JiraKey(value = "HHH-7446")
	public void testUniqueConstraintAnnotationOnNaturalIds() {
		Configuration configuration = new Configuration();
		ServiceRegistryUtil.applySettings( configuration.getStandardServiceRegistryBuilder() );
		configuration.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		configuration.addAnnotatedClass( Month.class );
		SessionFactory sf;
		try {
			sf = configuration.buildSessionFactory();
			sf.close();
		}
		catch (ConcurrentModificationException e) {
			fail( e.toString() );
		}
	}
}
