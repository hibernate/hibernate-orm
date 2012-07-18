package org.hibernate.jpa.test.util;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;


/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public abstract class CopyrightableContent {
	private Author author;

	public CopyrightableContent() {
	}

	public CopyrightableContent(Author author) {
		this.author = author;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	private Author getAuthor() {
		return author;
	}

	private void setAuthor(Author author) {
		this.author = author;
	}
}
