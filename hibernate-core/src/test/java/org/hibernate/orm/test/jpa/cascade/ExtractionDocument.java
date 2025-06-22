/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade;

import java.io.Serializable;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "portal_pk_docs_extraction")
@DynamicUpdate @DynamicInsert
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
