/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.build

import org.gradle.api.publish.maven.MavenPublication

/**
 * Defines the information we wish to put into the generated POM for publishing
 *
 * @author Steve Ebersole
 */
class MavenPublishingExtension {
	public static enum License {
		APACHE2,
		LGPL
	}

	// The Publications to which the pom information contained here should be supplied.
	// By default will apply to all.  This is used to limit them.
	def MavenPublication[] publications = []

	// information for the generated pom
	def String name;
	def String description;
	def License license;

	MavenPublication[] getPublications() {
		return publications
	}

	void setPublications(MavenPublication[] publications) {
		this.publications = publications
	}

	def publication(MavenPublication publication) {
		publications << publication
	}

	String getName() {
		return name
	}

	void setName(String name) {
		this.name = name
	}

	String getDescription() {
		return description
	}

	void setDescription(String description) {
		this.description = description
	}

	License getLicense() {
		return license
	}

	void setLicense(License license) {
		this.license = license
	}

	void license(Object license) {
		if ( license == null ) {
			setLicense( License.LGPL )
		}
		else if  ( license instanceof  License ) {
			setLicense( license as License )
		}
		else {
			setLicense( License.valueOf( license.toString().toUpperCase( Locale.ENGLISH ) ) )
		}
	}
}
