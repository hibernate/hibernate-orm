package org.hibernate.jpamodelgen.test.embeddedid.withinheritance;

import java.io.Serializable;

/**
 * @author Hardy Ferentschik
 */
public class AbstractRef implements Serializable {
	private final int id;

	protected AbstractRef() {
		// required by JPA
		id = 0;
	}

	protected AbstractRef(final int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}
}



