// $Id$
package org.hibernate.test.annotations.fkcircularity;
import java.io.Serializable;
import javax.persistence.ManyToOne;

/**
 * Test entities ANN-722.
 * 
 * @author Hardy Ferentschik
 *
 */
@SuppressWarnings("serial")
public class A_PK implements Serializable {
	public D d;

	@ManyToOne
	public D getD() {
		return d;
	}

	public void setD(D d) {
		this.d = d;
	}
}
