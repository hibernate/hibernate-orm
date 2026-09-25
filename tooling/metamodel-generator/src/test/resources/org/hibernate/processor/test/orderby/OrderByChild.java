package org.hibernate.processor.test.orderby;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class OrderByChild {

	@Id
	long id;

	int seqNo;
}
