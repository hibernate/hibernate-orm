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
package org.hibernate.metamodel.internal.source.annotations.xml.mocker;

import org.jboss.logging.Logger;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jaxb.spi.orm.JaxbAttributes;
import org.hibernate.jaxb.spi.orm.JaxbEntityListeners;
import org.hibernate.jaxb.spi.orm.JaxbIdClass;
import org.hibernate.jaxb.spi.orm.JaxbMappedSuperclass;
import org.hibernate.jaxb.spi.orm.JaxbPostLoad;
import org.hibernate.jaxb.spi.orm.JaxbPostPersist;
import org.hibernate.jaxb.spi.orm.JaxbPostRemove;
import org.hibernate.jaxb.spi.orm.JaxbPostUpdate;
import org.hibernate.jaxb.spi.orm.JaxbPrePersist;
import org.hibernate.jaxb.spi.orm.JaxbPreRemove;
import org.hibernate.jaxb.spi.orm.JaxbPreUpdate;

/**
 * Mock <mapped-superclass> to {@link javax.persistence.MappedSuperclass @MappedSuperClass}
 *
 * @author Strong Liu
 */
class MappedSuperclassMocker extends AbstractEntityObjectMocker {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			MappedSuperclassMocker.class.getName()
	);
	private JaxbMappedSuperclass mappedSuperclass;

	MappedSuperclassMocker(IndexBuilder indexBuilder, JaxbMappedSuperclass mappedSuperclass, EntityMappingsMocker.Default defaults) {
		super( indexBuilder, defaults );
		this.mappedSuperclass = mappedSuperclass;
	}

	@Override
	protected EntityElement getEntityElement() {
		return mappedSuperclass;
	}

	@Override
	protected void processExtra() {
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
