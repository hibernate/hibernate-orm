package org.hibernate.query.criteria.internal.hhh14916;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class Chapter {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long chapterId;

	public String name;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "book_id", nullable = false)
	public Book book;
}
