/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.softdelete.secondary;

import org.hibernate.annotations.SoftDelete;
import org.hibernate.type.YesNoConverter;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Basic;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * @implNote Uses YesNoConverter to work across all databases, even those
 * not supporting an actual BOOLEAN datatype
 *
 * @author Steve Ebersole
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "joined_root")
@SoftDelete(columnName = "removed", converter = YesNoConverter.class)
public abstract class JoinedRoot {
	@Id
	private Integer id;
	@Basic
	private String name;

	protected JoinedRoot() {
		// for Hibernate use
	}

	public JoinedRoot(Integer id, String name) {
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
}
