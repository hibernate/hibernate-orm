/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.tooling.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.util.ConfigureUtil

/**
 * Gradle DSL extension for configuring various Hibernate built-time tasks.  Registered
 * under "hibernate".
 *
 * @author Steve Ebersole
 */
class HibernateExtension {
	private final Project project

	/**
	 * The source sets that hold persistent model.  Default is project.sourceSets.main
	 */
	def SourceSet[]  sourceSets
	/**
	 * Configuration for bytecode enhancement.  Private; see instead {@link #enhance(groovy.lang.Closure)}
	 */
	protected EnhanceExtension enhance

	HibernateExtension(Project project) {
		this.project = project
		this.sourceSet( project.getConvention().getPlugin( JavaPluginConvention ).sourceSets.main )
	}

	/**
	 * Add a single SourceSet.
	 *
	 * @param sourceSet The SourceSet to add
	 */
	void sourceSet(SourceSet sourceSet) {
		if ( sourceSets == null ) {
			sourceSets = []
		}
		sourceSets += sourceSets
	}

	void enhance(Closure closure) {
		enhance = new EnhanceExtension()
		ConfigureUtil.configure( closure, enhance )
	}
}
