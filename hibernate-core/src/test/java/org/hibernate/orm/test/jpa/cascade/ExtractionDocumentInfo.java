/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreRemove;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "extraction_doc_info")
@DynamicUpdate @DynamicInsert
public class ExtractionDocumentInfo implements Serializable {
	private Long id;
	private Date lastModified;
	private Conference conference;
	private List<ExtractionDocument> documents;


	public ExtractionDocumentInfo() {
		lastModified = new Date();
	}

	public ExtractionDocumentInfo(Conference conference) {
		this.conference = conference;
		lastModified = new Date();
		documents = new ArrayList<ExtractionDocument>();
		documents.add( new ExtractionDocument( this ) );
	}

	@Transient
	public ExtractionDocument getDocument() {
		if ( documents.isEmpty() ) {
			documents.add( new ExtractionDocument( this ) );
		}
		Iterator<ExtractionDocument> iterator = documents.iterator();
		return iterator.next();
	}

	@Transient
	public byte[] getBody() {
		return getDocument().getBody();
	}

	public void setBody(byte[] body) {
		getDocument().setBody( body );
	}

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@OneToMany(mappedBy = "documentInfo", cascade = CascadeType.ALL)
	public List<ExtractionDocument> getDocuments() {
		return documents;
	}

	public void setDocuments(List<ExtractionDocument> documents) {
		this.documents = documents;
	}


	@ManyToOne
	@JoinColumn(name = "conference_id")
	public Conference getConference() {
		return conference;
	}

	public void setConference(Conference conference) {
		this.conference = conference;
	}

	@Column(name = "last_modified", nullable = false)
	public Date getLastModified() {
		return lastModified;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof ExtractionDocumentInfo ) ) return false;

		final ExtractionDocumentInfo newsInfo = (ExtractionDocumentInfo) o;

		return id.equals( newsInfo.id );
	}

	public int hashCode() {
		return id.hashCode();
	}

	@PreRemove
	public void preRemove() {
		getConference().setExtractionDocument( null );
	}
}
