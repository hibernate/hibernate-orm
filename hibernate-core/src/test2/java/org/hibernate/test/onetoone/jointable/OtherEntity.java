/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.onetoone.jointable;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Christian Beikov
 */
@Entity
public class OtherEntity {
	@Id
	private String id;

	public OtherEntity() {
	}

	public OtherEntity(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

}