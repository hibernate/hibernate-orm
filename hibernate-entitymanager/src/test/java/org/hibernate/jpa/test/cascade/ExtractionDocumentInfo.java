//$Id$
package org.hibernate.jpa.test.cascade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PreRemove;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "portal_pk_docs_extraction_info")
//@Cache(usage = READ_WRITE)
@DynamicInsert
@DynamicUpdate
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

