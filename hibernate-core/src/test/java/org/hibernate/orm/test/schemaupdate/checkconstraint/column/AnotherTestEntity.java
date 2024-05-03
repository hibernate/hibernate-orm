package org.hibernate.orm.test.schemaupdate.checkconstraint.column;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "AnotherTestEntity")
@Table(name = "ANOTHER_TEST_ENTITY")
public class AnotherTestEntity implements Another {
	@Id
	private Long id;

	@Column(name = "FIRST_NAME")
	private String firstName;
}