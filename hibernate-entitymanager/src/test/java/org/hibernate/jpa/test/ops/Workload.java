//$Id$
package org.hibernate.jpa.test.ops;
import javax.persistence.Column;
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
	@Column(name="load_")
	public Integer load;
}
