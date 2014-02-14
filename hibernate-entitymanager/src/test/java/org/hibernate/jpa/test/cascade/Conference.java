//$Id$
package org.hibernate.jpa.test.cascade;
import java.io.Serializable;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "portal_pk_conference")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.CHAR)
@DiscriminatorValue("X")
@DynamicInsert
@DynamicUpdate
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