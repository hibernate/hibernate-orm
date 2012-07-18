/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
