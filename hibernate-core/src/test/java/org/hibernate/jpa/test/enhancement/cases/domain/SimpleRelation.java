/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.enhancement.cases.domain;

/**
 * A simple entity relation used by {@link Simple} to ensure that enhanced
 * classes load all classes.
 * 
 * @author Dustin Schultz
 */
public class SimpleRelation {

	private String blah;

	public String getBlah() {
		return blah;
	}

	public void setBlah(String blah) {
		this.blah = blah;
	}

}
