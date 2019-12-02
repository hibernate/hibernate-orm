/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.compositeid;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;

@Entity(name = "Entity")
@IdClass(CompositeMoreFieldsId.class)
public class CompositeMoreFieldsIdIdentityEntity {

	@Id
	private Long id;

	@Id
	private Long anotherId;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long generatedId;

	public CompositeMoreFieldsId getId() {
		return new CompositeMoreFieldsId( id, anotherId, generatedId );
	}

	public void setId(CompositeMoreFieldsId id) {
		this.id = id.getId();
		this.anotherId = id.getAnotherId();
		this.generatedId = id.getGeneratedId();
	}
}
