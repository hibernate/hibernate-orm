/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.support.domains.basic;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
@Entity
@Audited
public class ScalePrecisionEntity {
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

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ScalePrecisionEntity that = (ScalePrecisionEntity) o;
		return Objects.equals( id, that.id ) &&
				Objects.equals( wholeNumber, that.wholeNumber );
	}

	@Override
	public int hashCode() {
		return Objects.hash( id, wholeNumber );
	}

	@Override
	public String toString() {
		return "ScalePrecisionEntity{" +
				"id=" + id +
				", wholeNumber=" + wholeNumber +
				'}';
	}

}
