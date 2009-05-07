package org.hibernate.test.annotations.generics;

/**
 * A test case for ANN-494.
 *
 * @author Edward Costello
 * @author Paolo Perrotta
 */

import java.util.HashSet;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

public class Classes {

	@Embeddable
	public static class Edition<T> {
		T name;
	}
	
	@Entity
	public static class Book {
		@Id
		@GeneratedValue(strategy=GenerationType.AUTO)
		Long id;
		
		@Embedded
		Edition<String> edition;
	}
	
	@Entity
	public static class PopularBook {
		@Id
		@GeneratedValue(strategy=GenerationType.AUTO)
		Long id;
		
		@ElementCollection
		Set<Edition<String>> editions = new HashSet<Edition<String>>();
	}
}
