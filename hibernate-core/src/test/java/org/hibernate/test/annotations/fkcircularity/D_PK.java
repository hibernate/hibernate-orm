package org.hibernate.test.annotations.fkcircularity;

import java.io.Serializable;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;

/**
 * Test entities ANN-722.
 * 
 * @author Hardy Ferentschik
 *
 */
@Embeddable
@SuppressWarnings("serial")
public class D_PK implements Serializable{
	private C c;
	
	@ManyToOne
	public C getC() {
		return c;
	}

	public void setC(C c) {
		this.c = c;
	}
}
