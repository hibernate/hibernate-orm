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
package org.hibernate.metamodel.binder.source.annotations.xml.mocker;

import org.jboss.jandex.ClassInfo;

import org.hibernate.metamodel.source.annotation.xml.XMLAccessType;
import org.hibernate.metamodel.source.annotation.xml.XMLEmbedded;

/**
 * @author Strong Liu
 */
class EmbeddedMocker extends PropertyMocker {
	private XMLEmbedded embedded;

	EmbeddedMocker(IndexBuilder indexBuilder, ClassInfo classInfo, EntityMappingsMocker.Default defaults, XMLEmbedded embedded) {
		super( indexBuilder, classInfo, defaults );
		this.embedded = embedded;
	}

	@Override
	protected void processExtra() {
		create( EMBEDDED );
		parserAttributeOverrides( embedded.getAttributeOverride(), getTarget() );
		parserAssociationOverrides( embedded.getAssociationOverride(), getTarget() );

	}

	@Override
	protected String getFieldName() {
		return embedded.getName();
	}

	@Override
	protected XMLAccessType getAccessType() {
		return embedded.getAccess();
	}

	@Override
	protected void setAccessType(XMLAccessType accessType) {
		embedded.setAccess( accessType );
	}
}
