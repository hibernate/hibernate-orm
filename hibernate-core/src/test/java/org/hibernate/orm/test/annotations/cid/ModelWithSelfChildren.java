/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.cid;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
@IdClass(ModelWithSelfChildrenId.class)
public class ModelWithSelfChildren implements Serializable {

	@Id
	private String string;
	@Id
	private int integer;

	@ManyToOne
	private ModelWithSelfChildren parent;

	@OneToMany(mappedBy = "parent")
	private List<ModelWithSelfChildren> children = new ArrayList<>();

	public String getString() {
		return string;
	}

	public void setString(String string) {
		this.string = string;
	}

	public int getInteger() {
		return integer;
	}

	public void setInteger(int integer) {
		this.integer = integer;
	}

	public ModelWithSelfChildren getParent() {
		return parent;
	}

	public void setParent(ModelWithSelfChildren parent) {
		this.parent = parent;
	}

	public List<ModelWithSelfChildren> getChildren() {
		return children;
	}

	public void setChildren(List<ModelWithSelfChildren> children) {
		this.children = children;
	}
}
