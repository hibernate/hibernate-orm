package org.hibernate.query.criteria.internal.hhh13908;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity(name = "Foo")
@Table(name = "Foo")
public class Foo {
	@Id
	Long id;
	String startTime;
}
