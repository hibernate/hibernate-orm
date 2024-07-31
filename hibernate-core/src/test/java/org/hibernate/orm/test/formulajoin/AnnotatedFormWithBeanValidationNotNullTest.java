/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.formulajoin;

import org.hibernate.cfg.Configuration;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@BaseUnitTest
public class AnnotatedFormWithBeanValidationNotNullTest {
	@Test
	@JiraKey( value = "HHH-8167" )
	public void testAnnotatedFormWithBeanValidationNotNull() {
		Configuration cfg = new Configuration();
		ServiceRegistryUtil.applySettings( cfg.getStandardServiceRegistryBuilder() );
		cfg.addAnnotatedClass( AnnotatedRoot.class ).addAnnotatedClass( AnnotatedDetail.class );
		cfg.buildSessionFactory().close();
	}
}
