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

import java.util.Collections;
import java.util.List;
import javax.persistence.AccessType;

import org.hibernate.metamodel.source.internal.jaxb.AttributesContainer;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEmbeddableAttributes;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEmbeddedId;
import org.hibernate.metamodel.source.internal.jaxb.JaxbId;
import org.hibernate.metamodel.source.internal.jaxb.JaxbVersion;

import org.jboss.jandex.ClassInfo;

/**
 * @author Strong Liu
 */
public class EmbeddableAttributesBuilder extends AbstractAttributesBuilder {
	private final JaxbEmbeddableAttributes attributes;

	EmbeddableAttributesBuilder(
			IndexBuilder indexBuilder,
			ClassInfo classInfo,
			AccessType accessType,
			Default defaults,
			JaxbEmbeddableAttributes embeddableAttributes) {
		super( indexBuilder, classInfo, defaults );
		this.attributes = embeddableAttributes;
	}

	@Override
	protected AttributesContainer getAttributesContainer() {
		return attributes;
	}

	@Override
	List<JaxbId> getId() {
		return Collections.emptyList();
	}

	@Override
	List<JaxbVersion> getVersion() {
		return Collections.emptyList();
	}

	@Override
	JaxbEmbeddedId getEmbeddedId() {
		return null;
	}
}
