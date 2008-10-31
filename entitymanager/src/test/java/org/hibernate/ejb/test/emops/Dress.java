//$Id: $
package org.hibernate.ejb.test.emops;

import javax.persistence.Id;
import javax.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Dress {
	@Id public String name;

}
