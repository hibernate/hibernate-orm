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

import java.util.List;

import javax.persistence.AccessType;

import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.metamodel.source.internal.jaxb.AttributesContainer;
import org.hibernate.metamodel.source.internal.jaxb.JaxbAny;
import org.hibernate.metamodel.source.internal.jaxb.JaxbAttributes;
import org.hibernate.metamodel.source.internal.jaxb.JaxbBasic;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEmbedded;
import org.hibernate.metamodel.source.internal.jaxb.JaxbEmbeddedId;
import org.hibernate.metamodel.source.internal.jaxb.JaxbId;
import org.hibernate.metamodel.source.internal.jaxb.JaxbManyToOne;
import org.hibernate.metamodel.source.internal.jaxb.JaxbNaturalId;
import org.hibernate.metamodel.source.internal.jaxb.JaxbVersion;
import org.jboss.jandex.ClassInfo;

/**
 * @author Strong Liu
 * @author Brett Meyer
 */
public class AttributesBuilder extends AbstractAttributesBuilder {
	private final JaxbAttributes attributes;

	AttributesBuilder(
			IndexBuilder indexBuilder,
			ClassInfo classInfo,
			AccessType accessType,
			Default defaults,
			JaxbAttributes attributes) {
		super( indexBuilder, classInfo, defaults );
		this.attributes = attributes;
	}
	
	@Override
	protected void parse() {
		if (getAttributesContainer() == null) {
			return;
		}
		
		super.parse();
		
		if ( attributes.getNaturalId() != null ) {
			final JaxbNaturalId naturalId = attributes.getNaturalId();
			// TODO: This is stupid.  Pieces of AbstractAttributesBuilder#parse should be pulled somewhere and
			// reused.
			for ( JaxbAny any : naturalId.getAny() ) {
				// TODO
			}
			for ( JaxbBasic basic : naturalId.getBasic() ) {
				new NaturalIdMocker( indexBuilder, classInfo, defaults, basic, naturalId.isMutable() ).process();
				new BasicMocker( indexBuilder, classInfo, defaults, basic ).process();
			}
			for ( JaxbEmbedded embedded : naturalId.getEmbedded() ) {
				new NaturalIdMocker( indexBuilder, classInfo, defaults, embedded, naturalId.isMutable() ).process();
				new EmbeddedMocker( indexBuilder, classInfo, defaults, embedded ).process();
			}
			for ( JaxbManyToOne manyToOne : naturalId.getManyToOne() ) {
				new NaturalIdMocker( indexBuilder, classInfo, defaults, manyToOne, naturalId.isMutable() ).process();
				new ManyToOneMocker( indexBuilder, classInfo, defaults, manyToOne ).process();
			}
			// TODO: @NaturalIdCache?
		}
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
