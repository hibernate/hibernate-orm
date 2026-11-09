package org.hibernate.processor.test.innerclass.hidden;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

class OtherModel {
	@Entity(name = "NestedBook")
	public static class Book {
		@Id
		long id;
	}

	@Entity(name = "HiddenBook")
	public static class HiddenBook {
		@Id
		long id;
	}
}
