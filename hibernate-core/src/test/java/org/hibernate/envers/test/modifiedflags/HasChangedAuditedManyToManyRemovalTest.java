/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.modifiedflags;

import java.util.Map;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.modifiedflags.Professor;
import org.hibernate.envers.test.support.domains.modifiedflags.Student;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-7510")
public class HasChangedAuditedManyToManyRemovalTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Student.class,
				Professor.class
		};
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.STORE_DATA_AT_DELETE, "true" );
		settings.put( EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, "true" );
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inJPA(
				entityManager -> {
					// Revision 1 - insertion
					Professor professor = new Professor();
					Student student = new Student();
					professor.getStudents().add( student );
					student.getProfessors().add( professor );
					entityManager.getTransaction().begin();
					entityManager.persist( professor );
					entityManager.persist( student );
					entityManager.getTransaction().commit();
					entityManager.clear();

					// Revision 2 - deletion
					entityManager.getTransaction().begin();
					professor = entityManager.find( Professor.class, professor.getId() );
					student = entityManager.find( Student.class, student.getId() );
					entityManager.remove( professor );
					entityManager.remove( student );
					// the issue is student.getProfessors() throws a LazyInitializationException.
					entityManager.getTransaction().commit();
				}
		);
	}

}
