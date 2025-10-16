/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.integration.modifiedflags.entities.Professor;
import org.hibernate.orm.test.envers.integration.modifiedflags.entities.Student;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-7510")
@EnversTest
@Jpa(
		annotatedClasses = { Student.class, Professor.class },
		integrationSettings = {
				@Setting(name = EnversSettings.STORE_DATA_AT_DELETE, value = "true"),
				@Setting(name = EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, value = "true")
		}
)
public class HasChangedAuditedManyToManyRemovalTest {

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			// Revision 1 - insertion
			Professor professor = new Professor();
			Student student = new Student();
			professor.getStudents().add( student );
			student.getProfessors().add( professor );
			em.persist( professor );
			em.persist( student );
		} );

		scope.inTransaction( em -> {
			// Revision 2 - deletion
			Professor professor = em.createQuery( "from Professor", Professor.class ).getSingleResult();
			Student student = em.createQuery( "from Student", Student.class ).getSingleResult();
			em.remove( professor );
			em.remove( student );
			// the issue is student.getProfessors() throws a LazyInitializationException.
		} );
	}
}
