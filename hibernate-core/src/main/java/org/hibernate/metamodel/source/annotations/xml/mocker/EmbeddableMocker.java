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
import org.hibernate.internal.jaxb.mapping.orm.JaxbAccessType;
import org.hibernate.internal.jaxb.mapping.orm.JaxbAttributes;
import org.hibernate.internal.jaxb.mapping.orm.JaxbEmbeddable;
import org.hibernate.internal.jaxb.mapping.orm.JaxbEntityListeners;
import org.hibernate.internal.jaxb.mapping.orm.JaxbIdClass;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPostLoad;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPostPersist;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPostRemove;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPostUpdate;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPrePersist;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPreRemove;
import org.hibernate.internal.jaxb.mapping.orm.JaxbPreUpdate;

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
	private JaxbEmbeddable embeddable;

	EmbeddableMocker(IndexBuilder indexBuilder, JaxbEmbeddable embeddable, EntityMappingsMocker.Default defaults) {
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
	protected JaxbIdClass getIdClass() {
		return null;
	}

	@Override
	protected JaxbEntityListeners getEntityListeners() {
		return null;
	}

	@Override
	protected JaxbAccessType getAccessType() {
		return embeddable.getAccess();
	}

	@Override
	protected String getClassName() {
		return embeddable.getClazz();
	}

	@Override
	protected JaxbPrePersist getPrePersist() {
		return null;
	}

	@Override
	protected JaxbPreRemove getPreRemove() {
		return null;
	}

	@Override
	protected JaxbPreUpdate getPreUpdate() {
		return null;
	}

	@Override
	protected JaxbPostPersist getPostPersist() {
		return null;
	}

	@Override
	protected JaxbPostUpdate getPostUpdate() {
		return null;
	}

	@Override
	protected JaxbPostRemove getPostRemove() {
		return null;
	}

	@Override
	protected JaxbPostLoad getPostLoad() {
		return null;
	}

	@Override
	protected JaxbAttributes getAttributes() {
		return null;
	}
}
