package org.hibernate.test.annotations.derivedidentities.e2.a;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(DependentId.class)
public class Dependent {
	@Id
	String name;
	
	@Id @ManyToOne
	@JoinColumns({
			@JoinColumn(name="FK1", referencedColumnName="firstName"),
			@JoinColumn(name="FK2", referencedColumnName="lastName")
	})
	Employee emp;
}
