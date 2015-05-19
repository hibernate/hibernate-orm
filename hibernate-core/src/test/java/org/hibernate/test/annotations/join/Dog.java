/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.join;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;

/**
 * @author Emmanuel Bernard
 */
@Entity
@SecondaryTable(
		name = "DogThoroughbred",
		pkJoinColumns = {@PrimaryKeyJoinColumn(name = "NAME", referencedColumnName = "name"),
		@PrimaryKeyJoinColumn(name = "OWNER_NAME", referencedColumnName = "ownerName")}
)
public class Dog {
	@Id
	public DogPk id;
	public int weight;
	@Column(table = "DogThoroughbred")
	public String thoroughbredName;

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Dog ) ) return false;

		final Dog dog = (Dog) o;

		if ( !id.equals( dog.id ) ) return false;

		return true;
	}

	public int hashCode() {
		return id.hashCode();
	}
}
