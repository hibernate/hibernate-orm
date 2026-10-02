package org.hibernate.processor.test.data.constraint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class MyEntity {

	@Id
	private Long id;
	@Column(unique = true)
	private String name;
	private Integer age;
}
