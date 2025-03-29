/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hhh12225;

public class VehicleTrackContract extends VehicleContract {
	public static final long serialVersionUID = 1L;
	private String _etchingId = null;
	private boolean _original = false;

	public String getEtchingId() {
		return _etchingId;
	}

	public void setEtchingId(String etchingId) {
		_etchingId = etchingId;
	}

	public boolean isOriginal() {
		return _original;
	}

	public void setOriginal(boolean original) {
		_original = original;
	}

}
