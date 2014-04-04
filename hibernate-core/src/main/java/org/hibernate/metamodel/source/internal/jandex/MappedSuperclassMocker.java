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

import org.hibernate.metamodel.source.internal.jaxb.JaxbAttributes;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntityListeners;
import org.hibernate.metamodel.source.internal.jaxb.JaxbIdClass;
import org.hibernate.metamodel.source.internal.jaxb.JaxbMappedSuperclass;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPostLoad;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPostPersist;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPostRemove;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPostUpdate;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPrePersist;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPreRemove;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPreUpdate;
import org.hibernate.metamodel.source.internal.jaxb.ManagedType;

/**
 * Mock <mapped-superclass> to {@link javax.persistence.MappedSuperclass @MappedSuperClass}
 *
 * @author Strong Liu
 */
public class MappedSuperclassMocker extends AbstractEntityObjectMocker {
	private JaxbMappedSuperclass mappedSuperclass;

	MappedSuperclassMocker(IndexBuilder indexBuilder, JaxbMappedSuperclass mappedSuperclass, Default defaults) {
		super( indexBuilder, defaults );
		this.mappedSuperclass = mappedSuperclass;
	}

	@Override
	protected ManagedType getEntityElement() {
		return mappedSuperclass;
	}

	@Override
	protected void doProcess() {
		create( MAPPED_SUPERCLASS );
	}

	@Override
	protected JaxbAttributes getAttributes() {
		return mappedSuperclass.getAttributes();
	}
	@Override
	protected boolean isExcludeDefaultListeners() {
		return mappedSuperclass.getExcludeDefaultListeners() != null;
	}

	@Override
	protected boolean isExcludeSuperclassListeners() {
		return mappedSuperclass.getExcludeSuperclassListeners() != null;
	}

	@Override
	protected JaxbIdClass getIdClass() {
		return mappedSuperclass.getIdClass();
	}

	@Override
	protected JaxbEntityListeners getEntityListeners() {
		return mappedSuperclass.getEntityListeners();
	}

	@Override
	protected JaxbPrePersist getPrePersist() {
		return mappedSuperclass.getPrePersist();
	}

	@Override
	protected JaxbPreRemove getPreRemove() {
		return mappedSuperclass.getPreRemove();
	}

	@Override
	protected JaxbPreUpdate getPreUpdate() {
		return mappedSuperclass.getPreUpdate();
	}

	@Override
	protected JaxbPostPersist getPostPersist() {
		return mappedSuperclass.getPostPersist();
	}

	@Override
	protected JaxbPostUpdate getPostUpdate() {
		return mappedSuperclass.getPostUpdate();
	}

	@Override
	protected JaxbPostRemove getPostRemove() {
		return mappedSuperclass.getPostRemove();
	}

	@Override
	protected JaxbPostLoad getPostLoad() {
		return mappedSuperclass.getPostLoad();
	}
}
