/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.support.domains.gambit;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Steve Ebersole
 */
@Entity
@SuppressWarnings("unused")
public class EntityWithManyToOneSelfReference {
	private Integer id;
	private String name;
	private Integer someInteger;
	private EntityWithManyToOneSelfReference other;

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne
	@JoinColumn
	public EntityWithManyToOneSelfReference getOther() {
		return other;
	}

	public void setOther(EntityWithManyToOneSelfReference other) {
		this.other = other;
	}
}
