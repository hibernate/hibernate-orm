//$Id$
package org.hibernate.test.annotations.indexcoll;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Gas {
	@Id
	@GeneratedValue
	public Integer id;
	public String name;

}
