/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.any;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.MetaValue;

@Entity
@Table( name = "lazy_property_set" )
public class LazyPropertySet {
	private Integer id;
	private String name;
	private Property someProperty;

	public LazyPropertySet() {
		super();
	}

	public LazyPropertySet(String name) {
		this.name = name;
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

	public void setName(String name) {
		this.name = name;
	}

	@Any( metaColumn = @Column( name = "property_type" ), fetch = FetchType.LAZY )
	@Cascade( value = { CascadeType.ALL } )
	@AnyMetaDef( idType = "integer", metaType = "string", metaValues = {
	@MetaValue( value = "S", targetEntity = StringProperty.class ),
	@MetaValue( value = "I", targetEntity = IntegerProperty.class )
			} )
	@JoinColumn( name = "property_id" )
	public Property getSomeProperty() {
		return someProperty;
	}

	public void setSomeProperty(Property someProperty) {
		this.someProperty = someProperty;
	}
}
