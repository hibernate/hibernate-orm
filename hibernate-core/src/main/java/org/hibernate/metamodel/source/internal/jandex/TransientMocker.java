/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
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
package org.hibernate.metamodel.source.internal.jandex;

import javax.persistence.AccessType;

import org.hibernate.metamodel.source.internal.jaxb.JaxbTransient;
import org.hibernate.metamodel.source.internal.jaxb.PersistentAttribute;

import org.jboss.jandex.ClassInfo;

/**
 * @author Strong Liu
 */
public class TransientMocker extends PropertyMocker {
	private final JaxbTransient transientObj;
	private final PersistentAttribute wrapper;

	TransientMocker(IndexBuilder indexBuilder, ClassInfo classInfo, Default defaults, final JaxbTransient transientObj) {
		super( indexBuilder, classInfo, defaults );
		this.transientObj = transientObj;
		this.wrapper = new PersistentAttribute() {
			@Override
			public String getName() {
				return transientObj.getName();
			}

			@Override
			public AccessType getAccess() {
				return AccessType.FIELD;
			}

			@Override
			public void setAccess(AccessType accessType) {
			}

			@Override
			public String getAttributeAccessor() {
				return null;
			}

			@Override
			public void setAttributeAccessor(String customAccess) {

			}
		};
	}

	@Override
	protected void doProcess() {
		create( TRANSIENT );
	}

	@Override
	protected PersistentAttribute getPersistentAttribute() {
		return wrapper;
	}
}
