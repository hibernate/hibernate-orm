/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.backref.map.compkey;
import java.io.Serializable;

/**
 * Part implementation
 *
 * @author Steve Ebersole
 */
public class Part implements Serializable {
	private String name;
	private String description;

	public Part() {
	}

	public Part(String name) {
		this.name = name;
	}

	public Part(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
