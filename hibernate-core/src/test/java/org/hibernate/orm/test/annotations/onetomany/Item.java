/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.Formula;

/**
 * @author Lukasz Antoniak
 */
@Entity
public class Item implements Serializable {
	@Id
	private int id;

	@Column( name = "code" )
	private String code;

	@Formula( "( SELECT LENGTH( code ) FROM DUAL )" )
	private int sortField;

	@ManyToOne
	private Box box;

	public Item() {
	}

	public Item(int id, String code, Box box) {
		this.id = id;
		this.code = code;
		this.box = box;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Item ) ) return false;

		Item item = (Item) o;

		if ( id != item.id ) return false;
		if ( sortField != item.sortField ) return false;
		if ( code != null ? !code.equals( item.code ) : item.code != null ) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = id;
		result = 31 * result + ( code != null ? code.hashCode() : 0 );
		result = 31 * result + sortField;
		return result;
	}

	@Override
	public String toString() {
		return "Item(id = " + id + ", code = " + code + ", sortField = " + sortField + ")";
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public int getSortField() {
		return sortField;
	}

	public void setSortField(int sortField) {
		this.sortField = sortField;
	}

	public Box getBox() {
		return box;
	}

	public void setBox(Box box) {
		this.box = box;
	}
}
