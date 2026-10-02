package org.hibernate.orm.test.jpa.metamodel.attributeInSuper;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;

/**
 * @author Hardy Ferentschik
 */
@Embeddable
@Access(AccessType.FIELD)
public class EmbeddableEntity {
	private String foo;
}
