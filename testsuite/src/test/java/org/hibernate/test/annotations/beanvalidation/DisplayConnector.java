package org.hibernate.test.annotations.beanvalidation;

import javax.persistence.Embeddable;
import javax.persistence.OneToOne;
import javax.persistence.CascadeType;
import javax.persistence.ManyToOne;
import javax.validation.Valid;
import javax.validation.constraints.Min;


/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class DisplayConnector {

	private int number;
	private Display display;

	@Min(1)
	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	@ManyToOne(cascade = CascadeType.PERSIST)
	@Valid
	public Display getDisplay() {
		return display;
	}

	public void setDisplay(Display display) {
		this.display = display;
	}
}
