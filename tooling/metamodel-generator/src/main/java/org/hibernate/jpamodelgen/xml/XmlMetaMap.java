/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
package org.hibernate.jpamodelgen.xml;

import org.hibernate.jpamodelgen.model.MetaEntity;

/**
 * @author Hardy Ferentschik
 */
public class XmlMetaMap extends XmlMetaCollection {

	private final String keyType;

	public XmlMetaMap(XmlMetaEntity parent, String propertyName, String type, String collectionType, String keyType) {
		super( parent, propertyName, type, collectionType );
		this.keyType = keyType;
	}

	public String getDeclarationString() {
		final MetaEntity hostingEntity = getHostingEntity();
		return new StringBuilder().append( "public static volatile " )
				.append( hostingEntity.importType( getMetaType() ) )
				.append( "<" )
				.append( hostingEntity.importType( hostingEntity.getQualifiedName() ) )
				.append( ", " )
				.append( hostingEntity.importType( keyType ) )
				.append( ", " )
				.append( hostingEntity.importType( getTypeDeclaration() ) )
				.append( "> " )
				.append( getPropertyName() )
				.append( ";" )
				.toString();
	}
}
