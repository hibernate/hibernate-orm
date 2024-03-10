/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.orm.test.annotations.inheritance.mixed;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.SecondaryTable;

/**
 * @author Emmanuel Bernard
 */
@Entity
@DiscriminatorValue("D")
@SecondaryTable(name = "DocumentMixed")
public class Document extends File {
	private int size;

	Document() {
	}

	Document(String name, int size) {
		super( name );
		this.size = size;
	}

	@Column(table = "DocumentMixed", name="doc_size", nullable = false)
	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}
}
