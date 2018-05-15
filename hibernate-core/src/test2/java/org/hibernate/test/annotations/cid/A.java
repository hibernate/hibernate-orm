/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cid;

import java.io.Serializable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

/**
 * @author Artur Legan
 */
@Entity
public class A implements Serializable{

	@EmbeddedId
	private AId aId;

	public AId getAId() {
		return aId;
	}

	public void setAId(AId aId) {
		this.aId = aId;
	}
}
