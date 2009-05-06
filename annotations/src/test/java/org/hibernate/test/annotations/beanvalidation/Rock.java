package org.hibernate.test.annotations.beanvalidation;

import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Rock extends Music {
	@NotNull
	public Integer bit;
}
