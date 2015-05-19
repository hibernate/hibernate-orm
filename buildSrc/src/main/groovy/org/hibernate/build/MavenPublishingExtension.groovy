/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
