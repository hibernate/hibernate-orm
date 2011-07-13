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

import org.hibernate.metamodel.source.annotation.jaxb.XMLBasic;
import org.hibernate.metamodel.source.annotation.jaxb.XMLElementCollection;
import org.hibernate.metamodel.source.annotation.jaxb.XMLEmbedded;
import org.hibernate.metamodel.source.annotation.jaxb.XMLEmbeddedId;
import org.hibernate.metamodel.source.annotation.jaxb.XMLId;
import org.hibernate.metamodel.source.annotation.jaxb.XMLManyToMany;
import org.hibernate.metamodel.source.annotation.jaxb.XMLManyToOne;
import org.hibernate.metamodel.source.annotation.jaxb.XMLOneToMany;
import org.hibernate.metamodel.source.annotation.jaxb.XMLOneToOne;
import org.hibernate.metamodel.source.annotation.jaxb.XMLTransient;
import org.hibernate.metamodel.source.annotation.jaxb.XMLVersion;

/**
 * Abstract Parser to handle {@link org.hibernate.metamodel.source.annotation.jaxb.XMLAttributes XMLAttributes}
 * and {@link org.hibernate.metamodel.source.annotation.jaxb.XMLEmbeddableAttributes XMLEmbeddableAttributes}.
 *
 * It would be really helpful if these two classes can implement an interface with those abstract methods in this class.
 *
 * @author Strong Liu
 */
abstract class AbstractAttributesBuilder {

	private ClassInfo classInfo;
	private EntityMappingsMocker.Default defaults;
	private IndexBuilder indexBuilder;

	AbstractAttributesBuilder(IndexBuilder indexBuilder, ClassInfo classInfo, EntityMappingsMocker.Default defaults) {
		this.indexBuilder = indexBuilder;
		this.classInfo = classInfo;
		this.defaults = defaults;
	}

	final void parser() {
		for ( XMLId id : getId() ) {
			new IdMocker( indexBuilder, classInfo, defaults, id ).process();
		}
		for ( XMLTransient transientObj : getTransient() ) {
			new TransientMocker( indexBuilder, classInfo, defaults, transientObj ).process();
		}
		for ( XMLVersion version : getVersion() ) {
			new VersionMocker( indexBuilder, classInfo, defaults, version ).process();
		}

		for ( XMLBasic basic : getBasic() ) {
			new BasicMocker( indexBuilder, classInfo, defaults, basic ).process();
		}
		for ( XMLElementCollection elementCollection : getElementCollection() ) {
			new ElementCollectionMocker(
					indexBuilder, classInfo, defaults, elementCollection
			).process();
		}
		for ( XMLEmbedded embedded : getEmbedded() ) {
			new EmbeddedMocker( indexBuilder, classInfo, defaults, embedded ).process();
		}
		for ( XMLManyToMany manyToMany : getManyToMany() ) {
			new ManyToManyMocker( indexBuilder, classInfo, defaults, manyToMany ).process();
		}

		for ( XMLManyToOne manyToOne : getManyToOne() ) {
			new ManyToOneMocker( indexBuilder, classInfo, defaults, manyToOne ).process();
		}
		for ( XMLOneToMany oneToMany : getOneToMany() ) {
			new OneToManyMocker(
					indexBuilder, classInfo, defaults, oneToMany
			).process();
		}
		for ( XMLOneToOne oneToOne : getOneToOne() ) {
			new OneToOneMocker( indexBuilder, classInfo, defaults, oneToOne ).process();
		}
		if ( getEmbeddedId() != null ) {
			new EmbeddedIdMocker(
					indexBuilder, classInfo, defaults, getEmbeddedId()
			).process();
		}
	}

	abstract List<XMLId> getId();

	abstract List<XMLTransient> getTransient();

	abstract List<XMLVersion> getVersion();

	abstract List<XMLBasic> getBasic();

	abstract List<XMLElementCollection> getElementCollection();

	abstract List<XMLEmbedded> getEmbedded();

	abstract List<XMLManyToMany> getManyToMany();

	abstract List<XMLManyToOne> getManyToOne();

	abstract List<XMLOneToMany> getOneToMany();

	abstract List<XMLOneToOne> getOneToOne();

	abstract XMLEmbeddedId getEmbeddedId();
}