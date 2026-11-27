package org.hibernate.orm.test.annotations.namingstrategy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "ContextParent")
@Table
public class ContextParent {
	@Id
	private Integer id;
}
