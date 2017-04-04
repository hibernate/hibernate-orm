/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: $

package org.hibernate.test.lob;
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
