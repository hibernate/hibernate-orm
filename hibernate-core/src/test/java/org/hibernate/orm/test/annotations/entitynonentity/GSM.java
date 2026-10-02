package org.hibernate.orm.test.annotations.entitynonentity;

import jakarta.persistence.Entity;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class GSM extends Cellular {
	int frequency;
}
