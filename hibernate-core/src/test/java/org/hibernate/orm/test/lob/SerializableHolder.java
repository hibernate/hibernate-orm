/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.lob;
import java.io.Serializable;

/**
 * An entity containing serializable data which is
 * mapped via the {@link org.hibernate.type.SerializableType}.
 *
 * @author Steve Ebersole
 */
public class SerializableHolder {
	private Long id;

	private Serializable serialData;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Serializable getSerialData() {
		return serialData;
	}

	public void setSerialData(Serializable serialData) {
		this.serialData = serialData;
	}
}
