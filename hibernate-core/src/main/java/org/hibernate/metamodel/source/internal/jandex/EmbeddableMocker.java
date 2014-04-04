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
import org.hibernate.metamodel.source.internal.jaxb.JaxbEmbeddable;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEntityListeners;
import org.hibernate.metamodel.source.internal.jaxb.JaxbIdClass;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPostLoad;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPostPersist;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPostRemove;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPostUpdate;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPrePersist;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPreRemove;
import org.hibernate.metamodel.source.internal.jaxb.JaxbPreUpdate;
import org.hibernate.metamodel.source.internal.jaxb.ManagedType;

/**
 * Mock <embeddable> to {@link javax.persistence.Embeddable @Embeddable}
 *
 * @author Strong Liu
 */
public class EmbeddableMocker extends AbstractEntityObjectMocker {
	private final JaxbEmbeddable embeddable;

	EmbeddableMocker(IndexBuilder indexBuilder, JaxbEmbeddable embeddable, Default defaults) {
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
	protected ManagedType getEntityElement() {
		return embeddable;
	}

	@Override
	protected void doProcess() {
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
