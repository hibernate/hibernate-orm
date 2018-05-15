/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.hhh12225;

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
