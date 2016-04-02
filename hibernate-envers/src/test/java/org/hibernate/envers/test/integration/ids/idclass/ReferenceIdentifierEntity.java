/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.ids.idclass;

import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author Matthew Morrissette (yinzara at gmail dot com)
 */
@Audited
@Entity
@IdClass(ReferenceIdentifierClassId.class)
public class ReferenceIdentifierEntity implements Serializable {

	@Id
	@JoinColumn(name="ClassTypeId", nullable=false)
	@GeneratedValue
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private IntegerGeneratedIdentityEntity iiie;

	@Id
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ClassTypeName")
	private ClassType type;

	private String sampleValue;

	public ReferenceIdentifierEntity() {
	}

	public ReferenceIdentifierEntity(ClassType type) {
		this.type = type;
	}

	public ReferenceIdentifierEntity(IntegerGeneratedIdentityEntity iiie, ClassType type) {
		this.iiie = iiie;
		this.type = type;
	}

	public ReferenceIdentifierEntity(IntegerGeneratedIdentityEntity iiie, ClassType type, String sampleValue) {
		this.iiie = iiie;
		this.type = type;
		this.sampleValue = sampleValue;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ReferenceIdentifierEntity) ) {
			return false;
		}

		ReferenceIdentifierEntity sampleClass = (ReferenceIdentifierEntity) o;

		if ( iiie != null ? !iiie.equals( sampleClass.iiie) : sampleClass.iiie != null ) {
			return false;
		}
		if ( type != null ? !type.equals( sampleClass.type ) : sampleClass.type != null ) {
			return false;
		}
		if ( sampleValue != null ? !sampleValue.equals( sampleClass.sampleValue ) : sampleClass.sampleValue != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = iiie != null ? iiie.hashCode() : 0;
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (sampleValue != null ? sampleValue.hashCode() : 0);
		return result;
	}

	public IntegerGeneratedIdentityEntity getIiie() {
		return iiie;
	}

	public void setIiie(IntegerGeneratedIdentityEntity iiie) {
		this.iiie = iiie;
	}

	public ClassType getType() {
		return type;
	}

	public void setType(ClassType type) {
		this.type = type;
	}

	public String getSampleValue() {
		return sampleValue;
	}

	public void setSampleValue(String sampleValue) {
		this.sampleValue = sampleValue;
	}
}
