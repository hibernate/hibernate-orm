/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.hashcode;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Basic;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Audited
public class WikiPage {
	@Id
	@GeneratedValue
	private Long id;

	@Basic
	private String title;

	@Basic
	private String content;

	@ElementCollection
	private Set<String> links = new HashSet<String>();

	@OneToMany
	private Set<WikiImage> images = new HashSet<WikiImage>();

	public WikiPage() {
	}

	public WikiPage(String title, String content) {
		this.title = title;
		this.content = content;
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Set<String> getLinks() {
		return links;
	}

	public void setLinks(Set<String> links) {
		this.links = links;
	}

	public Set<WikiImage> getImages() {
		return images;
	}

	public void setImages(Set<WikiImage> images) {
		this.images = images;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof WikiPage) ) {
			return false;
		}

		WikiPage wikiPage = (WikiPage) o;

		if ( content != null ? !content.equals( wikiPage.content ) : wikiPage.content != null ) {
			return false;
		}
		if ( title != null ? !title.equals( wikiPage.title ) : wikiPage.title != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = title != null ? title.hashCode() : 0;
		result = 31 * result + (content != null ? content.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "WikiPage{" +
				"title='" + title + '\'' +
				", content='" + content + '\'' +
				", links=" + links +
				", images=" + images +
				'}';
	}
}
