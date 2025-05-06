/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;


/**
 * Implementation of StateProvince.
 *
 * @author Steve Ebersole
 *
 * @deprecated Use {@link org.hibernate.testing.orm.domain.animal.StateProvince} instead
 */
@Deprecated
public class StateProvince {
	private Long id;
	private String name;
	private String isoCode;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIsoCode() {
		return isoCode;
	}

	public void setIsoCode(String isoCode) {
		this.isoCode = isoCode;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof StateProvince ) ) {
			return false;
		}

		StateProvince that = ( StateProvince ) o;

		if ( isoCode != null ? !isoCode.equals( that.getIsoCode() ) : that.getIsoCode() != null ) {
			return false;
		}
		if ( name != null ? !name.equals( that.getName() ) : that.getName() != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + ( isoCode != null ? isoCode.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "StateProvince{" +
				"id=" + id +
				", name='" + name + '\'' +
				", isoCode='" + isoCode + '\'' +
				'}';
	}
}
