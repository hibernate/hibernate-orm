/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metagen.mappedsuperclass.idclass;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

/**
 * @author Alexis Bataille
 * @author Steve Ebersole
 */
@MappedSuperclass
public abstract class AbstractAttribute implements Serializable {
	protected String key;
	protected String value;

	public AbstractAttribute() {
		super();
	}

	@Transient public abstract String getOwner();

	@Transient public String getKey() { return key; }

	public void setKey(String key) {
		this.key = key;
	}

	@Column(name = "attribute_value")
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
