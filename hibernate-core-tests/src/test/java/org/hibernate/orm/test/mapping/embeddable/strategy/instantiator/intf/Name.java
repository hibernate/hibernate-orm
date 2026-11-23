package org.hibernate.orm.test.mapping.embeddable.strategy.instantiator.intf;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;

/**
 * @author Steve Ebersole
 */
@Access( AccessType.PROPERTY )
public interface Name {
	String getFirstName();
	String getLastName();
}
