//$Id$
package org.hibernate.test.annotations.onetoone;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class TrousersZip {
	@Id
	public Integer id;
	@OneToOne(mappedBy = "zip")
	public Trousers trousers;
}
