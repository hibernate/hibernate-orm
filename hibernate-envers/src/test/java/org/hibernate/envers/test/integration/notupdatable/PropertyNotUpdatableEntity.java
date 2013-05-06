package org.hibernate.envers.test.integration.notupdatable;

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
public class PropertyNotUpdatableEntity implements Serializable {
	@Id
	@GeneratedValue
	private Long id;

	private String data;

	@Column(updatable = false)
	private String constantData1;

	@Column(updatable = false)
	private String constantData2;

	public PropertyNotUpdatableEntity() {
	}

	public PropertyNotUpdatableEntity(String data, String constantData1, String constantData2) {
		this.constantData1 = constantData1;
		this.constantData2 = constantData2;
		this.data = data;
	}

	public PropertyNotUpdatableEntity(String data, String constantData1, String constantData2, Long id) {
		this.data = data;
		this.id = id;
		this.constantData1 = constantData1;
		this.constantData2 = constantData2;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof PropertyNotUpdatableEntity) ) {
			return false;
		}

		PropertyNotUpdatableEntity that = (PropertyNotUpdatableEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		if ( constantData1 != null ? !constantData1.equals( that.constantData1 ) : that.constantData1 != null ) {
			return false;
		}
		if ( constantData2 != null ? !constantData2.equals( that.constantData2 ) : that.constantData2 != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (data != null ? data.hashCode() : 0);
		result = 31 * result + (constantData1 != null ? constantData1.hashCode() : 0);
		result = 31 * result + (constantData2 != null ? constantData2.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "PropertyNotUpdatableEntity(id = " + id + ", data = " + data + ", constantData1 = " + constantData1 + ", constantData2 = " + constantData2 + ")";
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getConstantData1() {
		return constantData1;
	}

	public void setConstantData1(String constantData1) {
		this.constantData1 = constantData1;
	}

	public String getConstantData2() {
		return constantData2;
	}

	public void setConstantData2(String constantData2) {
		this.constantData2 = constantData2;
	}
}
