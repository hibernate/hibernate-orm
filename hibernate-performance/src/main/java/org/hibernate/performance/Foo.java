package org.hibernate.performance;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.validation.constraints.Size;

@Entity
@Table
public class Foo {
	
	@Id
	@GeneratedValue
	@Column
	protected Long id;
	
	@Size(max = 90000)
	@Column(name = "message", length = 90000)
	@Lob
	protected String message;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}