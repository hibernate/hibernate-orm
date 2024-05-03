package org.hibernate.orm.test.schemaupdate.checkconstraint.table;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name = "AnotherTestEntity")
public class AnotherTestEntity implements Another {
	@Id
	private Long id;

	@Column(name = "FIRST_NAME")
	private String firstName;
}
