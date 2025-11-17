/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.components.relations;

import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.entities.StrTestNoProxyEntity;

/**
 * Do not mark as {@link Audited}. Should be implicitly treated as audited when part of audited entity.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Embeddable
@Table(name = "ManyToOneEagerComp")
public class ManyToOneEagerComponent {
	@ManyToOne(fetch = FetchType.EAGER)
	private StrTestNoProxyEntity entity;

	private String data;

	public ManyToOneEagerComponent(StrTestNoProxyEntity entity, String data) {
		this.entity = entity;
		this.data = data;
	}

	public ManyToOneEagerComponent() {
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public StrTestNoProxyEntity getEntity() {
		return entity;
	}

	public void setEntity(StrTestNoProxyEntity entity) {
		this.entity = entity;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ManyToOneEagerComponent) ) {
			return false;
		}

		ManyToOneEagerComponent that = (ManyToOneEagerComponent) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		if ( entity != null ? !entity.equals( that.entity ) : that.entity != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = entity != null ? entity.hashCode() : 0;
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "ManyToOneEagerComponent(data = " + data + ")";
	}
}
