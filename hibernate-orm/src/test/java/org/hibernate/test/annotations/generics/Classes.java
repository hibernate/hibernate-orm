/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.generics;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.Table;

public class Classes {

	@Embeddable
	@Table(name="Edition")
	public static class Edition<T> {
		T name;
	}
	
	
	@Entity
	@Table(name="Book")
	public static class Book {
		@Id
		@GeneratedValue(strategy=GenerationType.AUTO)
		Long id;
		
		@Embedded
		Edition<String> edition;
	}
	
	@Entity
	@Table(name="PopularBook")
	public static class PopularBook {
		@Id
		@GeneratedValue(strategy=GenerationType.AUTO)
		Long id;
		
		@ElementCollection
		@JoinTable(name="PopularBook_Editions",joinColumns={@JoinColumn(name="PopularBook_id")})

		Set<Edition<String>> editions = new HashSet<Edition<String>>();
	}
}
