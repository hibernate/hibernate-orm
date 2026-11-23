package org.hibernate.orm.test.jpa.pack.explicitpar;
import jakarta.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Washer {
	//No @id so picking it up should fail
}
