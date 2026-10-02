package org.hibernate.orm.test.inheritance.discriminator.joinedsubclass;

import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * @author Andrea Boriero
 */
@jakarta.persistence.Entity
@Inheritance(strategy = InheritanceType.JOINED)
public interface TestEntityInterface extends Common {
}
