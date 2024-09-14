/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hql.fetchAndJoin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "entity1")
public class Entity1 {
	@Id
	@GeneratedValue
	private long id;

	@ManyToOne
	@JoinColumn(name="entity2_id", nullable = false)
	private Entity2 entity2;

	@Column(name = "val")
	private String value;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Entity2 getEntity2() {
		return entity2;
	}

	public void setEntity2(Entity2 entity2) {
		this.entity2 = entity2;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
