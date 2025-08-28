/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Formula;

/**
 * @author Lukasz Antoniak
 */
@Entity
@Table(name = "folders")
public class Folder implements Serializable {
	@Id
	public Long id;

	public String name;

	@Formula("( SELECT CASE WHEN c.type = 'owner' THEN c.firstname + ' ' + c.lastname END FROM contacts c where c.folder_id = id )")
	public String owner;

	public Folder() {
	}

	public Folder(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( ! ( o instanceof Folder ) ) return false;

		Folder folder = (Folder) o;

		if ( id != null ? !id.equals( folder.id ) : folder.id != null ) return false;
		if ( name != null ? !name.equals( folder.name ) : folder.name != null ) return false;
		if ( owner != null ? !owner.equals( folder.owner ) : folder.owner != null ) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + ( name != null ? name.hashCode() : 0 );
		result = 31 * result + ( owner != null ? owner.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "Folder(id = " + id + ", name = " + name + ", owner = " + owner + ")";
	}
}
