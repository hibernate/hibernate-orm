/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.envers.integration.ids.embeddedid;

import java.io.Serializable;

import jakarta.persistence.Embeddable;

@Embeddable
public class CompositeEntityId implements Serializable{

	private String firstCode;
	private String secondCode;

	public String getFirstCode() {
		return firstCode;
	}

	public void setFirstCode(String firstCode) {
		this.firstCode = firstCode;
	}

	public String getSecondCode() {
		return secondCode;
	}

	public void setSecondCode(String secondCode) {
		this.secondCode = secondCode;
	}

	@Override
	public String toString() {
		return "CompositeEntityId{" +
				"firstCode='" + firstCode + '\'' +
				", secondCode='" + secondCode + '\'' +
				'}';
	}
}
