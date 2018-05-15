/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.schemagen;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * Test entity
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 */
@Entity(name = "Item")
public class Item implements Serializable {

	private String name;
	private String descr;

	public Item() {
	}

	public Item(String name, String desc) {
		this.name = name;
		this.descr = desc;
	}

	@Column(length = 200)
	public String getDescr() {
		return descr;
	}

	public void setDescr(String desc) {
		this.descr = desc;
	}

	@Id
	@Column(length = 30)
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
