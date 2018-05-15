/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.inheritance.joined;
import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.annotations.ForeignKey;

/**
 * @author Emmanuel Bernard
 */
@Entity
@ForeignKey(name = "FK_DOCU_FILE")
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
