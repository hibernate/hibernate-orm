/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cid;
import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author bartek
 */
@Entity
public class SomeEntity
		implements Serializable {

	@Id
	private SomeEntityId id;

	@Basic
	private String prop;

	/**
	 * @return the id
	 */
	public SomeEntityId getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(SomeEntityId id) {
		this.id = id;
	}

	/**
	 * @return the prop
	 */
	public String getProp() {
		return prop;
	}

	/**
	 * @param prop the prop to set
	 */
	public void setProp(String prop) {
		this.prop = prop;
	}

}
