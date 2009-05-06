package org.hibernate.test.annotations.beanvalidation;

import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Max;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class Button {

	private String name;

	private Integer size;

	@NotNull
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Max( 10 )
	public Integer getSize() {
		return size;
	}

	public void setSize(Integer size) {
		this.size = size;
	}
}
