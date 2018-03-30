/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.converter.custom;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity
@Table( name = "CUST_TYPE_CONV_ENTITY")
public class MyEntity {
	private Integer id;
	private MyCustomJavaType customType;

	public MyEntity() {
	}

	public MyEntity(Integer id, MyCustomJavaType customType) {
		this.id = id;
		this.customType = customType;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	// NOTE : this AttributeConverter should be auto-applied here

	@Basic
	public MyCustomJavaType getCustomType() {
		return customType;
	}

	public void setCustomType(MyCustomJavaType customType) {
		this.customType = customType;
	}
}
