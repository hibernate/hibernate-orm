/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.util.Date;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "portal_pk_conference")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.CHAR)
@DiscriminatorValue("X")
@DynamicUpdate @DynamicInsert
public class Conference implements Serializable {
	private Long id;
	private Date date;
	private ExtractionDocumentInfo extractionDocument;

	@OneToOne(mappedBy = "conference", cascade = CascadeType.ALL)
	public ExtractionDocumentInfo getExtractionDocument() {
		return extractionDocument;
	}

	public void setExtractionDocument(ExtractionDocumentInfo extractionDocument) {
		this.extractionDocument = extractionDocument;
	}


	public Conference() {
		date = new Date();
	}

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "c_date", nullable = false)
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( o == null || getClass() != o.getClass() ) return false;

		final Conference that = (Conference) o;

		return !( date != null ? !date.equals( that.date ) : that.date != null );

	}

	public int hashCode() {
		return ( date != null ? date.hashCode() : 0 );
	}
}
