package org.hibernate.orm.test.entitygraph.named.parsed.pkg3;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Person {
	@Id
	private Integer id;
	private String name;
}
