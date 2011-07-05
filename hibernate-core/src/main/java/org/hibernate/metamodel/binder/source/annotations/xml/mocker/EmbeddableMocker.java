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
package org.hibernate.metamodel.binder.source.annotations.xml.mocker;

import org.jboss.logging.Logger;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.source.annotation.xml.XMLAccessType;
import org.hibernate.metamodel.source.annotation.xml.XMLAttributes;
import org.hibernate.metamodel.source.annotation.xml.XMLEmbeddable;
import org.hibernate.metamodel.source.annotation.xml.XMLEntityListeners;
import org.hibernate.metamodel.source.annotation.xml.XMLIdClass;
import org.hibernate.metamodel.source.annotation.xml.XMLPostLoad;
import org.hibernate.metamodel.source.annotation.xml.XMLPostPersist;
import org.hibernate.metamodel.source.annotation.xml.XMLPostRemove;
import org.hibernate.metamodel.source.annotation.xml.XMLPostUpdate;
import org.hibernate.metamodel.source.annotation.xml.XMLPrePersist;
import org.hibernate.metamodel.source.annotation.xml.XMLPreRemove;
import org.hibernate.metamodel.source.annotation.xml.XMLPreUpdate;

/**
 * Mock <embeddable> to {@link javax.persistence.Embeddable @Embeddable}
 *
 * @author Strong Liu
 */
class EmbeddableMocker extends AbstractEntityObjectMocker {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			EmbeddableMocker.class.getName()
	);
	private XMLEmbeddable embeddable;

	EmbeddableMocker(IndexBuilder indexBuilder, XMLEmbeddable embeddable, EntityMappingsMocker.Default defaults) {
		super( indexBuilder, defaults );
		this.embeddable = embeddable;
	}

	@Override
	protected AbstractAttributesBuilder getAttributesBuilder() {
		if ( attributesBuilder == null ) {
			attributesBuilder = new EmbeddableAttributesBuilder(
					indexBuilder, classInfo, getAccessType(), getDefaults(), embeddable.getAttributes()
			);
		}
		return attributesBuilder;
	}

	@Override
	protected void processExtra() {
		create( EMBEDDABLE );
	}

	@Override
	protected void applyDefaults() {
		DefaultConfigurationHelper.INSTANCE.applyDefaults( embeddable, getDefaults() );
	}

	@Override
	protected boolean isMetadataComplete() {
		return embeddable.isMetadataComplete() != null && embeddable.isMetadataComplete();
	}

	@Override
	protected boolean isExcludeDefaultListeners() {
		return false;
	}

	@Override
	protected boolean isExcludeSuperclassListeners() {
		return false;
	}

	@Override
	protected XMLIdClass getIdClass() {
		return null;
	}

	@Override
	protected XMLEntityListeners getEntityListeners() {
		return null;
	}

	@Override
	protected XMLAccessType getAccessType() {
		return embeddable.getAccess();
	}

	@Override
	protected String getClassName() {
		return embeddable.getClazz();
	}

	@Override
	protected XMLPrePersist getPrePersist() {
		return null;
	}

	@Override
	protected XMLPreRemove getPreRemove() {
		return null;
	}

	@Override
	protected XMLPreUpdate getPreUpdate() {
		return null;
	}

	@Override
	protected XMLPostPersist getPostPersist() {
		return null;
	}

	@Override
	protected XMLPostUpdate getPostUpdate() {
		return null;
	}

	@Override
	protected XMLPostRemove getPostRemove() {
		return null;
	}

	@Override
	protected XMLPostLoad getPostLoad() {
		return null;
	}

	@Override
	protected XMLAttributes getAttributes() {
		return null;
	}
}
