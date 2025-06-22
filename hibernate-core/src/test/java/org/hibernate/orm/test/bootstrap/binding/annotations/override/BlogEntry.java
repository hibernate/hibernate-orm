/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.override;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;

/**
 * @author Lukasz Antoniak
 */
@Entity
@Table(schema = AssociationOverrideSchemaTest.SCHEMA_NAME)
@AssociationOverride(name = "tags",
		joinTable = @JoinTable(name = AssociationOverrideSchemaTest.TABLE_NAME,
				joinColumns = @JoinColumn(name = AssociationOverrideSchemaTest.ID_COLUMN_NAME),
				schema = AssociationOverrideSchemaTest.SCHEMA_NAME))
@AttributeOverride(name = "tags", column = @Column(name = AssociationOverrideSchemaTest.VALUE_COLUMN_NAME))
public class BlogEntry extends Entry {
	private String text;

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof BlogEntry ) ) return false;
		if ( !super.equals( o ) ) return false;

		BlogEntry blogEntry = (BlogEntry) o;

		if ( text != null ? !text.equals( blogEntry.text ) : blogEntry.text != null ) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + ( text != null ? text.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "BlogEntry(" + super.toString() + ", text = " + text + ")";
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
