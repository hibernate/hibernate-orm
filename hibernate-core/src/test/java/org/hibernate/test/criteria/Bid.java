package org.hibernate.test.criteria;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import java.io.Serializable;

/**
 * @author tknowlton at iamhisfriend dot org
 */
@Entity
public class Bid implements Serializable {
	@Id
	float amount;

	@Id
	@ManyToOne
	Item item;
}
