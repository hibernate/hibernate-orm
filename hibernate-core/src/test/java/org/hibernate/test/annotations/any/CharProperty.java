/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.any;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column;

@Entity
@Table( name = "char_property" )
public class CharProperty implements Property {
	private Integer id;

	private String name;

	private Character value;

	public CharProperty() {
		super();
	}

	public CharProperty(String name, Character value) {
		super();
		this.name = name;
		this.value = value;
	}

	public String asString() {
		return Character.toString( value );
	}

	public String getName() {
		return name;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "`value`")
	public Character getValue() {
		return value;
	}

	public void setValue(Character value) {
		this.value = value;
	}

	public void setName(String name) {
		this.name = name;
	}

}
