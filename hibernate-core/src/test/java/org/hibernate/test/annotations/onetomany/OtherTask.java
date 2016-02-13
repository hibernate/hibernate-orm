/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetomany;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Stephen Fikes
 */

@Entity
@Table(name = "othertask")
public class OtherTask extends TaskBase {
	private String otherName;

	public OtherTask(String otherName) {
		this();
		setOtherName( otherName );
	}

	public String getOtherName() {
		return otherName;
	}

	protected OtherTask() {
		super();
		// this form used by Hibernate
	}

	protected void setOtherName(String otherName) {
		this.otherName = otherName;
	}

	public int hashCode() {
		if ( otherName != null) {
			return otherName.hashCode();
		} else {
			return 0;
		}
	}

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (o instanceof OtherTask ) {
			OtherTask other = OtherTask.class.cast(o);
			if ( otherName != null) {
				return getOtherName().equals(other.getOtherName()) /* && getId() == other.getId() */;
			} else {
				return other.getOtherName() == null;
			}
		} else {
			return false;
		}
	}
}
