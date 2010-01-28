package org.hibernate.ejb.test.metadata;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class SimplePerson {
	@Id
	String ssn;
}
