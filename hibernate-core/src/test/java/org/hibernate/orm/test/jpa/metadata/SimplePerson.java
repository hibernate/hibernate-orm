package org.hibernate.orm.test.jpa.metadata;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class SimplePerson {
	@Id
	String ssn;
}
