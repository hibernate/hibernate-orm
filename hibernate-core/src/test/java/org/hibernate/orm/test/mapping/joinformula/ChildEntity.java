package org.hibernate.orm.test.mapping.joinformula;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ChildEntity {

	@Id
	private Long id;

	@Column(name = "PARENT_ID")
	private Long parentId;

	@Column
	private String name;

}
