package org.hibernate.test.annotations.derivedidentities.e1.c;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Dependent implements Serializable {

	@Id
	String name;


	@Id
	// id attribute mapped by join column default
	@ManyToOne
	Employee emp;

}
