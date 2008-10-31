//$Id: $
package org.hibernate.ejb.test.cascade;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Song {
	@Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ENTITY1_SEQ")
	@SequenceGenerator(name = "ENTITY1_SEQ") private Long id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Author author;

	public Author getAuthor() {
		return author;
	}

	public void setAuthor(Author author) {
		this.author = author;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

}
