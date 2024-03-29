/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.fromcore;

import jakarta.persistence.Entity;

/**
 * @author Steve Ebersole
 */
@Entity
public class SomeMappedSuperclassSubclass extends SomeMappedSuperclass {
	private String theData;

	public String getTheData() {
		return theData;
	}

	public void setTheData(String theData) {
		this.theData = theData;
	}
}
