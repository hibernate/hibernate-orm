package org.hibernate.test.annotations.derivedidentities.e1.a;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;


/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(DependentId.class)
public class Dependent {
	@Id
	String name;

	// id attribute mapped by join column default
	@Id
	@ManyToOne
	Employee emp;

	public Dependent() {
	}

	public Dependent(String name, Employee emp) {
		this.name = name;
		this.emp = emp;
	}
}
