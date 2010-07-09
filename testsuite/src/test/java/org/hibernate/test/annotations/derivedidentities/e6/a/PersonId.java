package org.hibernate.test.annotations.derivedidentities.e6.a;

import java.io.Serializable;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class PersonId implements Serializable {
	String firstName;
	String lastName;
}
