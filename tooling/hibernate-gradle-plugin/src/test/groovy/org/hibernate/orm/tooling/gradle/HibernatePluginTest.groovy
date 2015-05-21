/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.tooling.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

import org.junit.Test

import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

/**
 * Test what we can.  ProjectBuilder is better than nothing, but still quited limited in what
 * you can test (e.g. you cannot test task execution).
 *
 * @author Steve Ebersole
 */
class HibernatePluginTest {
	@Test
	public void testHibernatePluginAddsExtension() {
		Project project = ProjectBuilder.builder().build()
		project.plugins.apply 'org.hibernate.orm'

		assertNotNull( project.extensions.findByName( "hibernate" ) )
	}

	@Test
	public void testHibernateExtensionConfig() {
		Project project = ProjectBuilder.builder().build()
		project.plugins.apply 'org.hibernate.orm'

		project.extensions.findByType( HibernateExtension.class ).enhance {
			enableAssociationManagement = false
		}
	}
}
