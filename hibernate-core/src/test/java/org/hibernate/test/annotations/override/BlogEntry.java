/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.override;

import javax.persistence.AssociationOverride;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Table;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
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
