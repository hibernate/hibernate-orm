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

import org.hibernate.jaxb.spi.orm.JaxbBasic;
import org.hibernate.jaxb.spi.orm.JaxbElementCollection;
import org.hibernate.jaxb.spi.orm.JaxbEmbedded;
import org.hibernate.jaxb.spi.orm.JaxbEmbeddedId;
import org.hibernate.jaxb.spi.orm.JaxbId;
import org.hibernate.jaxb.spi.orm.JaxbManyToMany;
import org.hibernate.jaxb.spi.orm.JaxbManyToOne;
import org.hibernate.jaxb.spi.orm.JaxbOneToMany;
import org.hibernate.jaxb.spi.orm.JaxbOneToOne;
import org.hibernate.jaxb.spi.orm.JaxbTransient;
import org.hibernate.jaxb.spi.orm.JaxbVersion;

/**
 * Abstract parse to handle {@link org.hibernate.jaxb.spi.orm.JaxbAttributes JaxbAttributes}
 * and {@link org.hibernate.jaxb.spi.orm.JaxbEmbeddableAttributes JaxbEmbeddableAttributes}.
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

	final void parse() {
		for ( JaxbId id : getId() ) {
			new IdMocker( indexBuilder, classInfo, defaults, id ).process();
		}
		for ( JaxbTransient transientObj : getAttributesContainer().getTransient() ) {
			new TransientMocker( indexBuilder, classInfo, defaults, transientObj ).process();
		}
		for ( JaxbVersion version : getVersion() ) {
			new VersionMocker( indexBuilder, classInfo, defaults, version ).process();
		}

		for ( JaxbBasic basic : getAttributesContainer().getBasic() ) {
			new BasicMocker( indexBuilder, classInfo, defaults, basic ).process();
		}
		for ( JaxbElementCollection elementCollection : getAttributesContainer().getElementCollection() ) {
			new ElementCollectionMocker(
					indexBuilder, classInfo, defaults, elementCollection
			).process();
		}
		for ( JaxbEmbedded embedded : getAttributesContainer().getEmbedded() ) {
			new EmbeddedMocker( indexBuilder, classInfo, defaults, embedded ).process();
		}
		for ( JaxbManyToMany manyToMany : getAttributesContainer().getManyToMany() ) {
			new ManyToManyMocker( indexBuilder, classInfo, defaults, manyToMany ).process();
		}

		for ( JaxbManyToOne manyToOne : getAttributesContainer().getManyToOne() ) {
			new ManyToOneMocker( indexBuilder, classInfo, defaults, manyToOne ).process();
		}
		for ( JaxbOneToMany oneToMany : getAttributesContainer().getOneToMany() ) {
			new OneToManyMocker(
					indexBuilder, classInfo, defaults, oneToMany
			).process();
		}
		for ( JaxbOneToOne oneToOne : getAttributesContainer().getOneToOne() ) {
			new OneToOneMocker( indexBuilder, classInfo, defaults, oneToOne ).process();
		}
		if ( getEmbeddedId() != null ) {
			new EmbeddedIdMocker(
					indexBuilder, classInfo, defaults, getEmbeddedId()
			).process();
		}
	}

	abstract List<JaxbId> getId();
	abstract List<JaxbVersion> getVersion();
	abstract JaxbEmbeddedId getEmbeddedId();
	abstract protected AttributesContainer getAttributesContainer();
}