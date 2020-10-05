/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.idclass;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Entity
@IdClass( MyEntityId.class )
public class MyEntity {

	@Id
	private Long idA;

	@Id
	private Long idB;

	private String notes;

	public MyEntityId getId() {
		return new MyEntityId( idA, idB );
	}

	public void setId(MyEntityId id) {
		this.idA = id.getIdA();
		this.idB = id.getIdB();
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}
}
