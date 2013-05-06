package org.hibernate.envers.test.entities.components;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Audited
public class UniquePropsNotAuditedEntity {
	private Long id;
	private String data1;
	private String data2;

	public UniquePropsNotAuditedEntity() {
	}

	public UniquePropsNotAuditedEntity(Long id, String data1, String data2) {
		this.id = id;
		this.data1 = data1;
		this.data2 = data2;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		UniquePropsNotAuditedEntity that = (UniquePropsNotAuditedEntity) o;

		if ( data1 != null ? !data1.equals( that.data1 ) : that.data1 != null ) {
			return false;
		}
		if ( data2 != null ? !data2.equals( that.data2 ) : that.data2 != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (data1 != null ? data1.hashCode() : 0);
		result = 31 * result + (data2 != null ? data2.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "UniquePropsNotAuditedEntity(id = " + id + ", data1 = " + data1 + ", data2 = " + data2 + ")";
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

	@NotAudited
	public String getData2() {
		return data2;
	}

	public void setData2(String data2) {
		this.data2 = data2;
	}
}
