/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.callbacks.hbmxml;

import javax.persistence.EntityManagerFactory;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@Jpa(
		xmlMappings = "org/hibernate/orm/test/jpa/callbacks/hbmxml/ClassMappedMoreThanOnce.hbm.xml"
)
public class MappingClassMoreThanOnceTest {
	/**
	 * Tests that an entity manager can be created when a class is mapped more than once.
	 */
	@Test
	@TestForIssue(jiraKey = "HHH-8775")
	public void testBootstrapWithClassMappedMOreThanOnce(EntityManagerFactoryScope scope) {

		EntityManagerFactory emf = null;
		try {
			emf = scope.getEntityManagerFactory();
		}
		finally {
			if ( emf != null ) {
				try {
					emf.close();
				}
				catch (Exception ignore) {
				}
			}
		}
	}
}
