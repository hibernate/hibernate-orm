/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.boot.models.hbm.foreigngenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.OneToOne;

/**
 * @author Steve Ebersole
 */
@Entity
public class Thing {
	@Id
	private Integer id;
	@Basic
	private String name;
	@OneToOne(mappedBy = "owner")
	private Info info;

	protected Thing() {
		// for Hibernate use
	}

	public Thing(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Info getInfo() {
		return info;
	}

	public void setInfo(Info info) {
		this.info = info;
	}
}
