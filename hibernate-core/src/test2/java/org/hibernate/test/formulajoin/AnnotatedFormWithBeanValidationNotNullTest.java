/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.formulajoin;

import org.hibernate.cfg.Configuration;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Steve Ebersole
 */
public class AnnotatedFormWithBeanValidationNotNullTest extends BaseUnitTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-8167" )
	public void testAnnotatedFormWithBeanValidationNotNull() {
		Configuration cfg = new Configuration();
		cfg.addAnnotatedClass( AnnotatedMaster.class ).addAnnotatedClass( AnnotatedDetail.class );
		cfg.buildSessionFactory().close();
	}
}
