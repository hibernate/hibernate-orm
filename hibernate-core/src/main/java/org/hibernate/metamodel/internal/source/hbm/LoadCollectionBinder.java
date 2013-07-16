/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.metamodel.internal.source.hbm;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.cfg.HbmBinder;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryCollectionReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.jaxb.spi.hbm.JaxbLoadCollectionElement;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.source.LocalBindingContext;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
class LoadCollectionBinder extends AbstractReturnBinder<JaxbLoadCollectionElement> {
	LoadCollectionBinder(
			final JaxbLoadCollectionElement element, final int elementIndex, final LocalBindingContext context,
			final MetadataImplementor metadata) {
		super( element, elementIndex, context, metadata );
	}

	@Override
	NativeSQLQueryReturn process() {
		final String collectionAttribute = element.getRole();
		int dot = collectionAttribute.lastIndexOf( '.' );
		if ( dot == -1 ) {
			throw context.makeMappingException(
					"Collection attribute for sql query return [alias=" + alias + "] not formatted correctly {OwnerClassName.propertyName}"
			);
		}
		final String ownerClassName = HbmBinder.getClassName(
				collectionAttribute.substring( 0, dot ), context.getMappingDefaults().getPackageName()
		);
		final String ownerPropertyName = collectionAttribute.substring( dot + 1 );
		final Map<String, String[]> propertyResults = new HashMap<String, String[]>();
		processReturnProperties( new ReturnPropertiesCallbackImpl( alias, propertyResults, context ) );
		return new NativeSQLQueryCollectionReturn(
				alias, ownerClassName, ownerPropertyName, propertyResults, lockMode
		);
	}
}
