/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.compositeusertype.nested;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;

@Entity
public class LineEntity {
	private long id;

	@Id
	@GeneratedValue
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	private Line line;

	@Type(type = "org.hibernate.test.compositeusertype.nested.LineType")
	@Columns(columns = {
			@Column(name = "x1"),
			@Column(name = "y1"),
			@Column(name = "x2"),
			@Column(name = "y2")
	})
	public Line getLine() {
		return line;
	}

	public void setLine(Line line) {
		this.line = line;
	}
}
