package org.hibernate.envers.test.integration.basic;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class ScalePrecisionEntity implements Serializable {
	@Id
	@GeneratedValue
	private Long id;

	@Column(precision = 3, scale = 0)
	private Double wholeNumber;

	public ScalePrecisionEntity() {
	}

	public ScalePrecisionEntity(Double wholeNumber) {
		this.wholeNumber = wholeNumber;
	}

	public ScalePrecisionEntity(Double wholeNumber, Long id) {
		this.id = id;
		this.wholeNumber = wholeNumber;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ScalePrecisionEntity) ) {
			return false;
		}

		ScalePrecisionEntity that = (ScalePrecisionEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( wholeNumber != null ? !wholeNumber.equals( that.wholeNumber ) : that.wholeNumber != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (wholeNumber != null ? wholeNumber.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "ScalePrecisionEntity(id = " + id + ", wholeNumber = " + wholeNumber + ")";
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Double getWholeNumber() {
		return wholeNumber;
	}

	public void setWholeNumber(Double wholeNumber) {
		this.wholeNumber = wholeNumber;
	}
}
