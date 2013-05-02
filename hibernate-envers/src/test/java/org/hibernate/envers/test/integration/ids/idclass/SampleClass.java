package org.hibernate.envers.test.integration.ids.idclass;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.io.Serializable;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Audited
@Entity
@IdClass(RelationalClassId.class)
public class SampleClass implements Serializable {
	@Id
	@GeneratedValue
	private Long id;

	@Id
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ClassTypeName", referencedColumnName = "Name",
				insertable = true, updatable = true, nullable = false)
	private ClassType type;

	private String sampleValue;

	public SampleClass() {
	}

	public SampleClass(ClassType type) {
		this.type = type;
	}

	public SampleClass(Long id, ClassType type) {
		this.id = id;
		this.type = type;
	}

	public SampleClass(Long id, ClassType type, String sampleValue) {
		this.id = id;
		this.type = type;
		this.sampleValue = sampleValue;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof SampleClass) ) {
			return false;
		}

		SampleClass sampleClass = (SampleClass) o;

		if ( id != null ? !id.equals( sampleClass.id ) : sampleClass.id != null ) {
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
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (type != null ? type.hashCode() : 0);
		result = 31 * result + (sampleValue != null ? sampleValue.hashCode() : 0);
		return result;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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
