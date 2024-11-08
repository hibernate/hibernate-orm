package org.hibernate.orm.test.annotations.join;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.Collection;

@Entity
@Table(name = "BOOKS")
public class Book {

	private long bookId;

	private Collection<Page> pages;

	@Id
	@GeneratedValue
	@Column(name = "BOOK_ID")
	public long getBookId() {
		return bookId;
	}

	public void setBookId(long userid) {
		this.bookId = userid;
	}

	@OneToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "BOOK_PAGES",
			joinColumns = @JoinColumn(name = "T_BOOK_ID", referencedColumnName = "BOOK_ID"),
			inverseJoinColumns = @JoinColumn(name = "T_PAGE_ID", referencedColumnName = "PAGE_ID"))
	public Collection<Page> getPages() {
		return pages;
	}

	public void setPages(Collection<Page> groups) {
		this.pages = groups;
	}
}