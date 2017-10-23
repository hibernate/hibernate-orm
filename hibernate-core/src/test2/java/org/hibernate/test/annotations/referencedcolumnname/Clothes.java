/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.referencedcolumnname;
import javax.persistence.Entity;
import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Clothes {
	private Integer id;
	@Column(name = "`type`")
	private String type;
	private String flavor;

	public Clothes() {
	}

	public Clothes(String type, String flavor) {
		this.type = type;
		this.flavor = flavor;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getFlavor() {
		return flavor;
	}

	public void setFlavor(String flavor) {
		this.flavor = flavor;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Clothes ) ) return false;

		final Clothes clothes = (Clothes) o;

		if ( !flavor.equals( clothes.flavor ) ) return false;
		if ( !type.equals( clothes.type ) ) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = type.hashCode();
		result = 29 * result + flavor.hashCode();
		return result;
	}
}
