package org.hibernate.test.enums;

import java.io.Serializable;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class UnspecifiedEnumTypeEntity implements Serializable {
	public static enum E1 { X, Y }
	public static enum E2 { A, B }

	private Long id;

	private E1 enum1;

	private E2 enum2;

	public UnspecifiedEnumTypeEntity() {
	}

	public UnspecifiedEnumTypeEntity(E1 enum1, E2 enum2) {
		this.enum1 = enum1;
		this.enum2 = enum2;
	}

	public UnspecifiedEnumTypeEntity(E1 enum1, E2 enum2, Long id) {
		this.enum1 = enum1;
		this.enum2 = enum2;
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( ! ( o instanceof UnspecifiedEnumTypeEntity ) ) return false;

		UnspecifiedEnumTypeEntity that = (UnspecifiedEnumTypeEntity) o;

		if ( enum1 != that.enum1 ) return false;
		if ( enum2 != that.enum2 ) return false;
		if ( id != null ? !id.equals( that.id ) : that.id != null ) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + ( enum1 != null ? enum1.hashCode() : 0 );
		result = 31 * result + ( enum2 != null ? enum2.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "UnspecifiedEnumTypeEntity(id = " + id + ", enum1 = " + enum1 + ", enum2 = " + enum2 + ")";
	}

	public E1 getEnum1() {
		return enum1;
	}

	public void setEnum1(E1 enum1) {
		this.enum1 = enum1;
	}

	public E2 getEnum2() {
		return enum2;
	}

	public void setEnum2(E2 enum2) {
		this.enum2 = enum2;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}
