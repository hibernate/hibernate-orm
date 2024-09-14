/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.inheritance.joined;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.PrimaryKeyJoinColumn;

/**
 * @author Emmanuel Bernard
 */
@Entity
@PrimaryKeyJoinColumn(foreignKey = @ForeignKey(name = "FK_DOCU_FILE"))
public class Document extends File {
	@Column(nullable = false, name="xsize")
	private int size;

	Document() {
	}

	Document(String name, int size) {
		super( name );
		this.size = size;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
}
