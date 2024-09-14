/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * @author Sharath Reddy
 */
@FilterDef(name = "byCategory", parameters = {@ParamDef(name = "category", type = String.class)})
@Filter(name = "byCategory", condition = ":category = `CATEGORY`")
@MappedSuperclass
public class Tool extends Widget {

	@Column(name="`CATEGORY`")
	private String category;

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}


}
