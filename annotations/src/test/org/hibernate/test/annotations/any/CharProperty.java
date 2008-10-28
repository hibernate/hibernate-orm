package org.hibernate.test.annotations.any;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

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
