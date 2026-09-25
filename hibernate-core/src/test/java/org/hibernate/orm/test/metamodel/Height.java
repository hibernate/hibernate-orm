package org.hibernate.orm.test.metamodel;

import jakarta.persistence.Embeddable;

/**
 * @author Marco Belladelli
 */
@Embeddable
public class Height extends Measurement {
	private float height;
}
