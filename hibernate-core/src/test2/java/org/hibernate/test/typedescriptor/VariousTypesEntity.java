/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.typedescriptor;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
public class VariousTypesEntity implements Serializable {
	@Id
	private Integer id;

	private byte byteData;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public byte getByteData() {
		return byteData;
	}

	public void setByteData(byte byteData) {
		this.byteData = byteData;
	}
}
