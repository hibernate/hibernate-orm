package org.hibernate.metamodel.source.annotations.xml.mocker;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;

/**
 * @author Strong Liu
 */
@Entity
public class Author {
	private Long id;
	private String name;
	private List<Book> books = new ArrayList<Book>();

	@Id
	@GeneratedValue(generator = "SEQ_GEN")
	@SequenceGenerator(name = "SEQ_GEN", initialValue = 123)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@OneToMany(mappedBy = "author",cascade = CascadeType.MERGE)
	public List<Book> getBooks() {
		return books;
	}

	public void setBooks(List<Book> books) {
		this.books = books;
	}


}
