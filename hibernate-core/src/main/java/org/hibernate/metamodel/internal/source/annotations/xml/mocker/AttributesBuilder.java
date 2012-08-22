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

import java.util.List;

import org.jboss.jandex.ClassInfo;

import org.hibernate.jaxb.spi.orm.JaxbAccessType;
import org.hibernate.jaxb.spi.orm.JaxbAttributes;
import org.hibernate.jaxb.spi.orm.JaxbEmbeddedId;
import org.hibernate.jaxb.spi.orm.JaxbId;
import org.hibernate.jaxb.spi.orm.JaxbVersion;

/**
 * @author Strong Liu
 */
class AttributesBuilder extends AbstractAttributesBuilder {
	private final JaxbAttributes attributes;

	AttributesBuilder(IndexBuilder indexBuilder, ClassInfo classInfo, JaxbAccessType accessType, EntityMappingsMocker.Default defaults, JaxbAttributes attributes) {
		super( indexBuilder, classInfo, defaults );
		this.attributes = attributes;
	}

	@Override
	protected AttributesContainer getAttributesContainer() {
		return attributes;
	}

	@Override
	List<JaxbId> getId() {
		return attributes.getId();
	}

	@Override
	List<JaxbVersion> getVersion() {
		return attributes.getVersion();
	}

	@Override
	JaxbEmbeddedId getEmbeddedId() {
		return attributes.getEmbeddedId();
	}
}
