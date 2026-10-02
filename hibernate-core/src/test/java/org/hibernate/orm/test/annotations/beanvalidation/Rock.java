package org.hibernate.orm.test.annotations.beanvalidation;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Rock extends Music {
	@NotNull
	public Integer bit;
}
