package org.hibernate.test.instrument.domain;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity( name = "Address" )
@Table( name = "address" )
public class Address {
	private Integer id;

	private String text;

	public Address() {
	}

	public Address(Integer id, String text) {
		this.id = id;
		this.text = text;
	}

	@Id
	@Column( name = "oid" )
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
