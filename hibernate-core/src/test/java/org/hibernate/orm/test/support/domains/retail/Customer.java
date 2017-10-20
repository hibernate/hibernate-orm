/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.support.domains.retail;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Steve Ebersole
 */
@Entity
public class Customer {
	private Integer id;
	private String namee;

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getNamee() {
		return namee;
	}

	public void setNamee(String namee) {
		this.namee = namee;
	}
}
