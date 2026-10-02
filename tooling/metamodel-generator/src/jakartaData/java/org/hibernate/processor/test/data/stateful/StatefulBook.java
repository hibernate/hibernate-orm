package org.hibernate.processor.test.data.stateful;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class StatefulBook {
	@Id
	String isbn;
	String title;
}
