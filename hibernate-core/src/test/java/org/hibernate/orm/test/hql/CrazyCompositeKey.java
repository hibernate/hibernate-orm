/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;
import java.io.Serializable;

/**
 * Implementation of CrazyCompositeKey.
 *
 * @author Steve Ebersole
 */
public class CrazyCompositeKey implements Serializable {
	private Long id;
	private Long otherId;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getOtherId() {
		return otherId;
	}

	public void setOtherId(Long otherId) {
		this.otherId = otherId;
	}

	public boolean equals(Object that) {
		CrazyCompositeKey cck = (CrazyCompositeKey) that;
		return cck.id.longValue() == id.longValue()
			&& cck.otherId.longValue() == otherId.longValue();
	}

	public int hashCode() {
		return id.hashCode() + otherId.hashCode();
	}
}
