/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.modifiedflags;

import java.util.Map;

import jakarta.persistence.EntityManager;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.integration.modifiedflags.entities.Professor;
import org.hibernate.orm.test.envers.integration.modifiedflags.entities.Student;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-7510")
public class HasChangedAuditedManyToManyRemovalTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Student.class,
				Professor.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( EnversSettings.STORE_DATA_AT_DELETE, "true" );
		options.put( EnversSettings.GLOBAL_WITH_MODIFIED_FLAG, "true" );
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager entityManager = getEntityManager();
		try {
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
		finally {
			entityManager.close();
		}
	}

}
