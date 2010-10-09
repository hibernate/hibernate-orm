package org.hibernate.test.annotations.derivedidentities.e1.b;

import java.io.Serializable;
import javax.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class DependentId implements Serializable {
	String name;
	long empPK;	// corresponds to PK type of Employee
}
