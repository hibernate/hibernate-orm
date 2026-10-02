package org.hibernate.orm.test.jpa.emops;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Dress {
	@Id public String name;

}
