//$Id$
package org.hibernate.test.annotations.embedded;
import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.hibernate.annotations.Parent;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class Summary {
	private int size;
	private String text;
	private Book summarizedBook;

	@Column(name="summary_size")
	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@Parent
	public Book getSummarizedBook() {
		return summarizedBook;
	}

	public void setSummarizedBook(Book summarizedBook) {
		this.summarizedBook = summarizedBook;
	}
}
