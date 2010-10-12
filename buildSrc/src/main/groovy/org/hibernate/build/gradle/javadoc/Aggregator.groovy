/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */

package org.hibernate.build.gradle.javadoc

import org.gradle.api.tasks.SourceSet
import org.gradle.api.Project

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
class Aggregator {
	private final Javadoc javadocTask;
	private Set<String> excludedSourceSetNames;

	public Aggregator(Javadoc javadocTask) {
		this.javadocTask = javadocTask;
	}

	private Set<String> getExcludedSourceSetNames() {
		if ( excludedSourceSetNames == null ) {
			excludedSourceSetNames = new HashSet<String>();
		}
		return excludedSourceSetNames;
	}

	/**
	 * Allow adding them one by one
	 *
	 */
	public void excludeSourceSetName(String name) {
		getExcludedSourceSetNames().add( name );
	}

	/**
	 * Also, allow adding them all at once
	 */
	public void excludeSourceSetNames(String[] names) {
		getExcludedSourceSetNames().addAll( Arrays.asList( names ) );
	}

	public void project(Project project) {
		project.sourceSets.each { SourceSet sourceSet ->
            if ( excludedSourceSetNames == null || !excludedSourceSetNames.contains( sourceSet.name ) ) {
                javadocTask.source sourceSet.allJava
                if( javadocTask.classpath ) {
                    javadocTask.classpath += sourceSet.classes + sourceSet.compileClasspath
                }
                else {
                    javadocTask.classpath = sourceSet.classes + sourceSet.compileClasspath
                }
            }
        }
	}
}
