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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

@Entity
public class Book {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long bookId;

	@Column
	public String name;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "author_id", nullable = false)
	public Author author;

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "book", orphanRemoval = true, cascade = CascadeType.ALL)
	public List<Chapter> chapters = new ArrayList<>();
}
