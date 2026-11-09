package org.hibernate.processor.test.innerclass.hidden;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

public class OtherModel {
	@Entity(name = "NestedBook")
	public static class Book {
		@Id
		long id;
	}

	@Entity
	public static class Article {
		@Id
		long id;
		Hidden.Category category;
	}

	static class Hidden {
		public enum Category { FICTION, HISTORY }

		@Entity(name = "HiddenBook")
		public static class Book {
			@Id
			long id;
		}
	}
}
