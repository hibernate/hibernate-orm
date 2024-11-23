/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement.recreate;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.Date;

/**
 * @author Sergey Astakhov
 */
@Embeddable
public class PoiArrival {

	@Temporal(TemporalType.TIMESTAMP)
	private Date expectedTime;

	@Temporal(TemporalType.TIMESTAMP)
	private Date arriveTime;

	public Date getExpectedTime() {
		return expectedTime;
	}

	public void setExpectedTime(Date _expectedTime) {
		expectedTime = _expectedTime;
	}

	public Date getArriveTime() {
		return arriveTime;
	}

	public void setArriveTime(Date _arriveTime) {
		arriveTime = _arriveTime;
	}

}
