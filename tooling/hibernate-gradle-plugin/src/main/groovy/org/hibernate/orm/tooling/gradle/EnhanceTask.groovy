/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.tooling.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.util.ConfigureUtil

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
class EnhanceTask extends DefaultTask {
	@Input
	EnhanceExtension options
	@InputFiles
	SourceSet[] sourceSets

	@TaskAction
	void enhance() {
		for ( SourceSet sourceSet: sourceSets ) {
			EnhancementHelper.enhance( sourceSet, options, project )
		}
	}

	void options(Closure closure) {
		options = new EnhanceExtension()
		ConfigureUtil.configure( closure, options )
	}
}
