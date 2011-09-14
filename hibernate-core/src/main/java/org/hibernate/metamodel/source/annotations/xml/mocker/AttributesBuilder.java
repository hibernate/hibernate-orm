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

import java.util.List;

import org.jboss.jandex.ClassInfo;

import org.hibernate.internal.jaxb.mapping.orm.JaxbAccessType;
import org.hibernate.internal.jaxb.mapping.orm.JaxbAttributes;
import org.hibernate.internal.jaxb.mapping.orm.JaxbBasic;
import org.hibernate.internal.jaxb.mapping.orm.JaxbElementCollection;
import org.hibernate.internal.jaxb.mapping.orm.JaxbEmbedded;
import org.hibernate.internal.jaxb.mapping.orm.JaxbEmbeddedId;
import org.hibernate.internal.jaxb.mapping.orm.JaxbId;
import org.hibernate.internal.jaxb.mapping.orm.JaxbManyToMany;
import org.hibernate.internal.jaxb.mapping.orm.JaxbManyToOne;
import org.hibernate.internal.jaxb.mapping.orm.JaxbOneToMany;
import org.hibernate.internal.jaxb.mapping.orm.JaxbOneToOne;
import org.hibernate.internal.jaxb.mapping.orm.JaxbTransient;
import org.hibernate.internal.jaxb.mapping.orm.JaxbVersion;

/**
 * @author Strong Liu
 */
class AttributesBuilder extends AbstractAttributesBuilder {
	private JaxbAttributes attributes;

	AttributesBuilder(IndexBuilder indexBuilder, ClassInfo classInfo, JaxbAccessType accessType, EntityMappingsMocker.Default defaults, JaxbAttributes attributes) {
		super( indexBuilder, classInfo, defaults );
		this.attributes = attributes;
	}

	@Override
	List<JaxbBasic> getBasic() {
		return attributes.getBasic();
	}

	@Override
	List<JaxbId> getId() {
		return attributes.getId();
	}

	@Override
	List<JaxbTransient> getTransient() {
		return attributes.getTransient();
	}

	@Override
	List<JaxbVersion> getVersion() {
		return attributes.getVersion();
	}

	@Override
	List<JaxbElementCollection> getElementCollection() {
		return attributes.getElementCollection();
	}

	@Override
	List<JaxbEmbedded> getEmbedded() {
		return attributes.getEmbedded();
	}

	@Override
	List<JaxbManyToMany> getManyToMany() {
		return attributes.getManyToMany();
	}

	@Override
	List<JaxbManyToOne> getManyToOne() {
		return attributes.getManyToOne();
	}

	@Override
	List<JaxbOneToMany> getOneToMany() {
		return attributes.getOneToMany();
	}

	@Override
	List<JaxbOneToOne> getOneToOne() {
		return attributes.getOneToOne();
	}

	@Override
	JaxbEmbeddedId getEmbeddedId() {
		return attributes.getEmbeddedId();
	}
}
