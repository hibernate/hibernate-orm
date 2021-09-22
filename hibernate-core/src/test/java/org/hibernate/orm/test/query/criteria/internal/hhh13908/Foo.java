package org.hibernate.orm.test.query.criteria.internal.hhh13908;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity(name = "Foo")
@Table(name = "Foo")
public class Foo {
	@Id
	Long id;
	String startTime;
}
