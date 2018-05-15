/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$

package org.hibernate.test.annotations.access.xml;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Hardy Ferentschik
 */
@Entity
public class Tourist {
	@Id
	@GeneratedValue
	private long id;

	private String name;

	public String getDestination() {
		return destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private String destination;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
}


