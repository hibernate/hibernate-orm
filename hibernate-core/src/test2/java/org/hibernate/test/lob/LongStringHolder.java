/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: $

package org.hibernate.test.lob;


/**
 * An entity containing data that is materialized into a String immediately.
 *
 * @author Gail Badner
 */
public class LongStringHolder {
	private Long id;

	private String longString;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getLongString() {
		return longString;
	}

	public void setLongString(String longString) {
		this.longString = longString;
	}
}
