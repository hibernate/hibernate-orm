package org.hibernate.test.bytecode.enhancement.transform;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;

@Entity
public class Child {
	@EmbeddedId
	ChildKey id;

	@MapsId("parent")
	@ManyToOne
	Parent parent;

	public String shouldNotTransformIdFieldAccess() {
		return id.parent;
	}
}
