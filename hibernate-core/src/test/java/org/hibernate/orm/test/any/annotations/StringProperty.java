/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.any.annotations;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

@Entity
@Table(name="string_property")
public class StringProperty implements Property {
	private Integer id;
	private String name;
	private String value;

	public StringProperty() {
		super();
	}

	public StringProperty(String name, String value) {
		super();
		this.name = name;
		this.value = value;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public String asString() {
		return value;
	}

	@Column(name = "`value`")
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public void setName(String name) {
		this.name = name;
	}
}
