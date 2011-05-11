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

import org.hibernate.metamodel.source.annotation.xml.XMLAccessType;
import org.hibernate.metamodel.source.annotation.xml.XMLAttributes;
import org.hibernate.metamodel.source.annotation.xml.XMLBasic;
import org.hibernate.metamodel.source.annotation.xml.XMLElementCollection;
import org.hibernate.metamodel.source.annotation.xml.XMLEmbedded;
import org.hibernate.metamodel.source.annotation.xml.XMLEmbeddedId;
import org.hibernate.metamodel.source.annotation.xml.XMLId;
import org.hibernate.metamodel.source.annotation.xml.XMLManyToMany;
import org.hibernate.metamodel.source.annotation.xml.XMLManyToOne;
import org.hibernate.metamodel.source.annotation.xml.XMLOneToMany;
import org.hibernate.metamodel.source.annotation.xml.XMLOneToOne;
import org.hibernate.metamodel.source.annotation.xml.XMLTransient;
import org.hibernate.metamodel.source.annotation.xml.XMLVersion;

/**
 * @author Strong Liu
 */
class AttributesBuilder extends AbstractAttributesBuilder {
	private XMLAttributes attributes;

	AttributesBuilder(IndexBuilder indexBuilder, ClassInfo classInfo, XMLAccessType accessType, EntityMappingsMocker.Default defaults, XMLAttributes attributes) {
		super( indexBuilder, classInfo, defaults );
		this.attributes = attributes;
	}

	@Override
	List<XMLBasic> getBasic() {
		return attributes.getBasic();
	}

	@Override
	List<XMLId> getId() {
		return attributes.getId();
	}

	@Override
	List<XMLTransient> getTransient() {
		return attributes.getTransient();
	}

	@Override
	List<XMLVersion> getVersion() {
		return attributes.getVersion();
	}

	@Override
	List<XMLElementCollection> getElementCollection() {
		return attributes.getElementCollection();
	}

	@Override
	List<XMLEmbedded> getEmbedded() {
		return attributes.getEmbedded();
	}

	@Override
	List<XMLManyToMany> getManyToMany() {
		return attributes.getManyToMany();
	}

	@Override
	List<XMLManyToOne> getManyToOne() {
		return attributes.getManyToOne();
	}

	@Override
	List<XMLOneToMany> getOneToMany() {
		return attributes.getOneToMany();
	}

	@Override
	List<XMLOneToOne> getOneToOne() {
		return attributes.getOneToOne();
	}

	@Override
	XMLEmbeddedId getEmbeddedId() {
		return attributes.getEmbeddedId();
	}


}
