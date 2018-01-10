/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.queryconstant;

import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Gail Badner
 */
public class NonConventionalJavaConstantAllowedTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { AnEntity.class };
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void addSettings(Map settings) {
		super.addSettings( settings );
		settings.put( AvailableSettings.CONVENTIONAL_JAVA_CONSTANTS, "false" );
	}

	@Before
	public void setup() {
		Session s = openSession();
		s.getTransaction().begin();

		AnEntity anEntity = new AnEntity();
		anEntity.operationType = OPERATION_TYPE.CREATE;

		s.persist( anEntity );

		s.getTransaction().commit();
		s.clear();
	}

	@After
	public void cleanup() {
		Session s = openSession();
		s.getTransaction().begin();

		s.createQuery( "delete from AnEntity" ).executeUpdate();

		s.getTransaction().commit();
		s.clear();
	}


	@Test
	@TestForIssue( jiraKey = "HHH-4959")
	public void testDisabled() {
		assertFalse( sessionFactory().getSessionFactoryOptions().isConventionalJavaConstants() );

		Session s = openSession();
		s.getTransaction().begin();

		AnEntity anEntity = (AnEntity) s.createQuery(
				"from AnEntity e where e.operationType = org.hibernate.test.queryconstant.NonConventionalJavaConstantDefaultTest$OPERATION_TYPE.CREATE"
		).uniqueResult();

		assertNotNull( anEntity );
		s.getTransaction().commit();
		s.close();
	}

	public enum OPERATION_TYPE {
		CREATE, UPDATE, DELETE
	}

	@Entity(name = "AnEntity")
	public static class AnEntity {
		@Id
		@GeneratedValue
		private long id;

		OPERATION_TYPE operationType;
	}
}
