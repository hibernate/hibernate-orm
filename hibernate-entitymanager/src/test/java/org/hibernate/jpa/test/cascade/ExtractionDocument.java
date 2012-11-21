//$Id$
package org.hibernate.jpa.test.cascade;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Proxy;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "portal_pk_docs_extraction")
//@Cache(usage = READ_WRITE)
@DynamicInsert
@DynamicUpdate
@Proxy
public class ExtractionDocument implements Serializable {
	private Long id;
	private byte[] body;
	private ExtractionDocumentInfo documentInfo;

	public ExtractionDocument() {
	}

	public ExtractionDocument(ExtractionDocumentInfo documentInfo) {
		this.documentInfo = documentInfo;
	}


	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@OneToOne
	@JoinColumn(name = "document_info_id")
	public ExtractionDocumentInfo getDocumentInfo() {
		return documentInfo;
	}

	public void setDocumentInfo(ExtractionDocumentInfo documentInfo) {
		this.documentInfo = documentInfo;
	}

	@Column(nullable = false)
	public byte[] getBody() {
		return body;
	}

	public void setBody(byte[] body) {
		this.body = body;
	}
}
