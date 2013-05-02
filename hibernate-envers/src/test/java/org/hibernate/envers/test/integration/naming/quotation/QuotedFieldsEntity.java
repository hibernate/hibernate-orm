package org.hibernate.envers.test.integration.naming.quotation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
public class QuotedFieldsEntity {
	@Id
	@GeneratedValue
	@Column(name = "`id`")
	private Long id;

	@Column(name = "`data1`")
	@Audited
	private String data1;

	@Column(name = "`data2`")
	@Audited
	private Integer data2;

	public QuotedFieldsEntity() {
	}

	public QuotedFieldsEntity(String data1, Integer data2) {
		this.data1 = data1;
		this.data2 = data2;
	}

	public QuotedFieldsEntity(Long id, String data1, Integer data2) {
		this.id = id;
		this.data1 = data1;
		this.data2 = data2;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getData1() {
		return data1;
	}

	public void setData1(String data1) {
		this.data1 = data1;
	}

	public Integer getData2() {
		return data2;
	}

	public void setData2(Integer data2) {
		this.data2 = data2;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof QuotedFieldsEntity) ) {
			return false;
		}

		QuotedFieldsEntity that = (QuotedFieldsEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( data1 != null ? !data1.equals( that.data1 ) : that.data1 != null ) {
			return false;
		}
		if ( data2 != null ? !data2.equals( that.data2 ) : that.data2 != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (data1 != null ? data1.hashCode() : 0);
		result = 31 * result + (data2 != null ? data2.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "QuotedFieldsEntity{" +
				"id=" + id +
				", data1='" + data1 + '\'' +
				", data2=" + data2 +
				'}';
	}
}
