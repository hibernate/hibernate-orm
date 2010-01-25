package org.hibernate.test.annotations.derivedidentities.e4.a;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Person {
	@Id
	String ssn;
}