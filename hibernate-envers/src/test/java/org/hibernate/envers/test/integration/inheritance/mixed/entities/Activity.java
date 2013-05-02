package org.hibernate.envers.test.integration.inheritance.mixed.entities;

import java.io.Serializable;

public interface Activity extends Serializable {
	ActivityId getId();

	Integer getSequenceNumber();
}
