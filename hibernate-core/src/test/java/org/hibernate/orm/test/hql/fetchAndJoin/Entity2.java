/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hql.fetchAndJoin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "entity2")
public class Entity2 {
	@Id
	@GeneratedValue
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="entity3_id")
	private Entity3 entity3;

	@Column(name = "val")
	private String value;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Entity3 getEntity3() {
		return entity3;
	}

	public void setEntity3(Entity3 entity3) {
		this.entity3 = entity3;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
