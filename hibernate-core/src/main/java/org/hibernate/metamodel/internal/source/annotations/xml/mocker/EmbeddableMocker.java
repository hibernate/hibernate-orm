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

import org.hibernate.jaxb.spi.orm.JaxbAttributes;
import org.hibernate.jaxb.spi.orm.JaxbEmbeddable;
import org.hibernate.jaxb.spi.orm.JaxbEntityListeners;
import org.hibernate.jaxb.spi.orm.JaxbIdClass;
import org.hibernate.jaxb.spi.orm.JaxbPostLoad;
import org.hibernate.jaxb.spi.orm.JaxbPostPersist;
import org.hibernate.jaxb.spi.orm.JaxbPostRemove;
import org.hibernate.jaxb.spi.orm.JaxbPostUpdate;
import org.hibernate.jaxb.spi.orm.JaxbPrePersist;
import org.hibernate.jaxb.spi.orm.JaxbPreRemove;
import org.hibernate.jaxb.spi.orm.JaxbPreUpdate;

/**
 * Mock <embeddable> to {@link javax.persistence.Embeddable @Embeddable}
 *
 * @author Strong Liu
 */
class EmbeddableMocker extends AbstractEntityObjectMocker {
	private final JaxbEmbeddable embeddable;

	EmbeddableMocker(IndexBuilder indexBuilder, JaxbEmbeddable embeddable, EntityMappingsMocker.Default defaults) {
		super( indexBuilder, defaults );
		this.embeddable = embeddable;
	}

	@Override
	protected AbstractAttributesBuilder getAttributesBuilder() {
		if ( attributesBuilder == null ) {
			attributesBuilder = new EmbeddableAttributesBuilder(
					indexBuilder, classInfo, embeddable.getAccess(), getDefaults(), embeddable.getAttributes()
			);
		}
		return attributesBuilder;
	}

	@Override
	protected EntityElement getEntityElement() {
		return embeddable;
	}

	@Override
	protected void processExtra() {
		create( EMBEDDABLE );
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
