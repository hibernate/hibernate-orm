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
 * Abstract Parser to handle {@link org.hibernate.internal.jaxb.mapping.orm.JaxbAttributes JaxbAttributes}
 * and {@link org.hibernate.internal.jaxb.mapping.orm.JaxbEmbeddableAttributes JaxbEmbeddableAttributes}.
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
		for ( JaxbId id : getId() ) {
			new IdMocker( indexBuilder, classInfo, defaults, id ).process();
		}
		for ( JaxbTransient transientObj : getTransient() ) {
			new TransientMocker( indexBuilder, classInfo, defaults, transientObj ).process();
		}
		for ( JaxbVersion version : getVersion() ) {
			new VersionMocker( indexBuilder, classInfo, defaults, version ).process();
		}

		for ( JaxbBasic basic : getBasic() ) {
			new BasicMocker( indexBuilder, classInfo, defaults, basic ).process();
		}
		for ( JaxbElementCollection elementCollection : getElementCollection() ) {
			new ElementCollectionMocker(
					indexBuilder, classInfo, defaults, elementCollection
			).process();
		}
		for ( JaxbEmbedded embedded : getEmbedded() ) {
			new EmbeddedMocker( indexBuilder, classInfo, defaults, embedded ).process();
		}
		for ( JaxbManyToMany manyToMany : getManyToMany() ) {
			new ManyToManyMocker( indexBuilder, classInfo, defaults, manyToMany ).process();
		}

		for ( JaxbManyToOne manyToOne : getManyToOne() ) {
			new ManyToOneMocker( indexBuilder, classInfo, defaults, manyToOne ).process();
		}
		for ( JaxbOneToMany oneToMany : getOneToMany() ) {
			new OneToManyMocker(
					indexBuilder, classInfo, defaults, oneToMany
			).process();
		}
		for ( JaxbOneToOne oneToOne : getOneToOne() ) {
			new OneToOneMocker( indexBuilder, classInfo, defaults, oneToOne ).process();
		}
		if ( getEmbeddedId() != null ) {
			new EmbeddedIdMocker(
					indexBuilder, classInfo, defaults, getEmbeddedId()
			).process();
		}
	}

	abstract List<JaxbId> getId();

	abstract List<JaxbTransient> getTransient();

	abstract List<JaxbVersion> getVersion();

	abstract List<JaxbBasic> getBasic();

	abstract List<JaxbElementCollection> getElementCollection();

	abstract List<JaxbEmbedded> getEmbedded();

	abstract List<JaxbManyToMany> getManyToMany();

	abstract List<JaxbManyToOne> getManyToOne();

	abstract List<JaxbOneToMany> getOneToMany();

	abstract List<JaxbOneToOne> getOneToOne();

	abstract JaxbEmbeddedId getEmbeddedId();
}