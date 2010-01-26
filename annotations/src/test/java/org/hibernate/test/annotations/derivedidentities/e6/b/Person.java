package org.hibernate.test.annotations.derivedidentities.e6.b;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Person {
	@EmbeddedId
	PersonId id;
}