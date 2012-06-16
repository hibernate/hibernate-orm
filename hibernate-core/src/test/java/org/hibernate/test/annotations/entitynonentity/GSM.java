//$Id$
package org.hibernate.test.annotations.entitynonentity;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class GSM extends Cellular {
	int frequency;
}
