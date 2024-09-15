package org.hibernate.processor.test.data.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Thing {
	@Id long id;
}
