/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id:$

package org.hibernate.jpa.test.exception;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PrePersist;


/**
 * @author Hardy Ferentschik
 */
@Entity
public class Instrument {

	@Id
	@GeneratedValue
	private int id;

	private String name;

	private Type type;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	@PrePersist
	public void prePersist() {
		throw new RuntimeException( "Instrument broken." );
	}

	public enum Type {
		WIND, STRINGS, PERCUSSION
	}
}


