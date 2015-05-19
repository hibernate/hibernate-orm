/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.entity;

import static org.junit.Assert.fail;

import java.util.ConcurrentModificationException;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Guenther Demetz
 */
public class HibernateAnnotationMappingTest extends BaseUnitTestCase {
	
	@Test
	@TestForIssue( jiraKey = "HHH-7446" )
	public void testUniqueConstraintAnnotationOnNaturalIds() throws Exception {
		Configuration configuration = new Configuration();
		configuration.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		configuration.addAnnotatedClass(Month.class);
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
