/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.any.annotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyKeyJavaClass;

@Entity
@Table(name = "property_holder")
public class ImplicitPropertyHolder {

	@Id
	private Integer id;

	@Any
	@AnyKeyJavaClass(Integer.class)
	@Column(name = "property_type")
	@JoinColumn(name = "property_id")
	private Property property;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Property getProperty() {
		return property;
	}

	public void setProperty(Property property) {
		this.property = property;
	}
}
