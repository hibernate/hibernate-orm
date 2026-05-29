/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pagination;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.DynamicUpdate;

/**
 * @author Piotr Findeisen
 */
@Entity
@DynamicUpdate
public class DataMetaPoint {
	@Id
	@GeneratedValue
	private long id;
	@ManyToOne(fetch = FetchType.LAZY)
	private DataPoint dataPoint;
	private String meta;

	/**
	 * @return Returns the id.
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param id
	 *            The id to set.
	 */
	public void setId(long id) {
		this.id = id;
	}

	public DataPoint getDataPoint() {
		return dataPoint;
	}

	public void setDataPoint(DataPoint dataPoint) {
		this.dataPoint = dataPoint;
	}

	public String getMeta() {
		return meta;
	}

	public void setMeta(String meta) {
		this.meta = meta;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dataPoint == null) ? 0 : dataPoint.hashCode());
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + ((meta == null) ? 0 : meta.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		DataMetaPoint dataPoint = (DataMetaPoint) o;

		if (meta != null ? !meta.equals(dataPoint.meta) : dataPoint.meta != null) {
			return false;
		}
		if (dataPoint != null ? !dataPoint.equals(dataPoint.dataPoint) : dataPoint.dataPoint != null) {
			return false;
		}

		return true;
	}
}
