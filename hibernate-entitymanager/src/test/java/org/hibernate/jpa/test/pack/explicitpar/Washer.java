//$Id$
package org.hibernate.jpa.test.pack.explicitpar;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Washer {
	//No @id so picking it up should fail
}
