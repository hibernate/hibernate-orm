/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.lob;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

/**
 * @author Lukasz Antoniak
 */
@Entity
public class Document implements Serializable {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private Integer revision;

	@Lob
	@Column(length = 5000)
	private String fullText;

	@Column(length = 120)
	private String shortDescription;

	public Document() {
	}

	public Document(Integer revision, String shortDescription, String fullText) {
		this.revision = revision;
		this.shortDescription = shortDescription;
		this.fullText = fullText;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Document ) ) return false;

		Document document = (Document) o;

		if ( fullText != null ? !fullText.equals( document.fullText ) : document.fullText != null ) return false;
		if ( id != null ? !id.equals( document.id ) : document.id != null ) return false;
		if ( revision != null ? !revision.equals( document.revision ) : document.revision != null ) return false;
		if ( shortDescription != null ? !shortDescription.equals( document.shortDescription ) : document.shortDescription != null ) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + ( revision != null ? revision.hashCode() : 0 );
		result = 31 * result + ( shortDescription != null ? shortDescription.hashCode() : 0 );
		result = 31 * result + ( fullText != null ? fullText.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "Document(id = " + id + ", revision = " + revision + ", shortDescription = "
				+ shortDescription + ", fullText = " + fullText + ")";
	}

	public String getFullText() {
		return fullText;
	}

	public void setFullText(String fullText) {
		this.fullText = fullText;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getRevision() {
		return revision;
	}

	public void setRevision(Integer revision) {
		this.revision = revision;
	}

	public String getShortDescription() {
		return shortDescription;
	}

	public void setShortDescription(String shortDescription) {
		this.shortDescription = shortDescription;
	}
}
