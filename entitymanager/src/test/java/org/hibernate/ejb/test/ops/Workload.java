//$Id$
package org.hibernate.ejb.test.ops;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Workload {
	@Id
	@GeneratedValue
	public Integer id;
	public String name;
	public Integer load;
}
