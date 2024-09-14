/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.refcolnames.secondary;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import org.hibernate.annotations.NaturalId;

@Entity
@SecondaryTable(name = "secondary_split")
public class Split {
	@Id
	@GeneratedValue
	@Column(name = "id")
	private Long id;

	private String name;

	@Column(table = "secondary_split")
	private String description;

	@NaturalId
	@Column(table = "secondary_split")
	private int code;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public Long getId() {
		return id;
	}
}
