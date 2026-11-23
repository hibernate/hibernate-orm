package org.hibernate.orm.test.annotations.entitynonentity;

import jakarta.persistence.MappedSuperclass;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public class Phone extends Voice {
	boolean isNumeric;
}
