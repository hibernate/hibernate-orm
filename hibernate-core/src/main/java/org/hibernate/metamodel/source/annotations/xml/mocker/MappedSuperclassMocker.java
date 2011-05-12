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
package org.hibernate.metamodel.source.annotations.xml.mocker;

import org.jboss.logging.Logger;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.source.annotation.xml.XMLAccessType;
import org.hibernate.metamodel.source.annotation.xml.XMLAttributes;
import org.hibernate.metamodel.source.annotation.xml.XMLEntityListeners;
import org.hibernate.metamodel.source.annotation.xml.XMLIdClass;
import org.hibernate.metamodel.source.annotation.xml.XMLMappedSuperclass;
import org.hibernate.metamodel.source.annotation.xml.XMLPostLoad;
import org.hibernate.metamodel.source.annotation.xml.XMLPostPersist;
import org.hibernate.metamodel.source.annotation.xml.XMLPostRemove;
import org.hibernate.metamodel.source.annotation.xml.XMLPostUpdate;
import org.hibernate.metamodel.source.annotation.xml.XMLPrePersist;
import org.hibernate.metamodel.source.annotation.xml.XMLPreRemove;
import org.hibernate.metamodel.source.annotation.xml.XMLPreUpdate;

/**
 * Mock <mapped-superclass> to {@link javax.persistence.MappedSuperclass @MappedSuperClass}
 * @author Strong Liu
 */
class MappedSuperclassMocker extends AbstractEntityObjectMocker {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			MappedSuperclassMocker.class.getName()
	);
	private XMLMappedSuperclass mappedSuperclass;

	MappedSuperclassMocker(IndexBuilder indexBuilder, XMLMappedSuperclass mappedSuperclass, EntityMappingsMocker.Default defaults) {
		super( indexBuilder, defaults );
		this.mappedSuperclass = mappedSuperclass;
	}
	@Override
	protected void applyDefaults() {
		String className = MockHelper.buildSafeClassName( mappedSuperclass.getClazz(), getDefaults().getPackageName() );
		mappedSuperclass.setClazz( className );
		if ( mappedSuperclass.isMetadataComplete() == null ) {
			mappedSuperclass.setMetadataComplete( getDefaults().getMetadataComplete() );
		}
		LOG.debugf( "Adding XML overriding information for %s", className );
	}

	@Override
	protected void processExtra() {
		create( MAPPED_SUPERCLASS );
	}

	@Override
	protected XMLAttributes getAttributes() {
		return mappedSuperclass.getAttributes();
	}

	@Override
	protected XMLAccessType getAccessType() {
		return mappedSuperclass.getAccess();
	}

	@Override
	protected boolean isMetadataComplete() {
		return mappedSuperclass.isMetadataComplete() != null && mappedSuperclass.isMetadataComplete();
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
	protected XMLIdClass getIdClass() {
		return mappedSuperclass.getIdClass();
	}

	@Override
	protected XMLEntityListeners getEntityListeners() {
		return mappedSuperclass.getEntityListeners();
	}

	protected String getClassName() {
		return mappedSuperclass.getClazz();
	}

	@Override
	protected XMLPrePersist getPrePersist() {
		return mappedSuperclass.getPrePersist();
	}

	@Override
	protected XMLPreRemove getPreRemove() {
		return mappedSuperclass.getPreRemove();
	}

	@Override
	protected XMLPreUpdate getPreUpdate() {
		return mappedSuperclass.getPreUpdate();
	}

	@Override
	protected XMLPostPersist getPostPersist() {
		return mappedSuperclass.getPostPersist();
	}

	@Override
	protected XMLPostUpdate getPostUpdate() {
		return mappedSuperclass.getPostUpdate();
	}

	@Override
	protected XMLPostRemove getPostRemove() {
		return mappedSuperclass.getPostRemove();
	}

	@Override
	protected XMLPostLoad getPostLoad() {
		return mappedSuperclass.getPostLoad();
	}


}
