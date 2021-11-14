package org.hibernate.query.criteria.internal.hhh14916;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
public class Author {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long authorId;

	@Column
	public String name;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "author", orphanRemoval = true, cascade = CascadeType.ALL)
	public List<Book> books = new ArrayList<>();
}
