/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOneToManyCollectionElementType;
import org.hibernate.boot.model.source.spi.PluralAttributeElementNature;
import org.hibernate.boot.model.source.spi.PluralAttributeElementSourceOneToMany;
import org.hibernate.boot.model.source.spi.PluralAttributeSource;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeElementSourceOneToManyImpl
		extends AbstractPluralAssociationElementSourceImpl
		implements PluralAttributeElementSourceOneToMany {
	private final JaxbHbmOneToManyCollectionElementType jaxbOneToManyElement;
	private final String referencedEntityName;
	private final String cascadeString;

	public PluralAttributeElementSourceOneToManyImpl(
			MappingDocument mappingDocument,
			final PluralAttributeSource pluralAttributeSource,
			final JaxbHbmOneToManyCollectionElementType jaxbOneToManyElement,
			String cascadeString) {
		super( mappingDocument, pluralAttributeSource );
		this.jaxbOneToManyElement = jaxbOneToManyElement;
		this.cascadeString = cascadeString;

		this.referencedEntityName = StringHelper.isNotEmpty( jaxbOneToManyElement.getEntityName() )
				? jaxbOneToManyElement.getEntityName()
				: mappingDocument.qualifyClassName( jaxbOneToManyElement.getClazz() );
	}

	@Override
	public PluralAttributeElementNature getNature() {
		return PluralAttributeElementNature.ONE_TO_MANY;
	}

	@Override
	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	@Override
	public boolean isIgnoreNotFound() {
		return jaxbOneToManyElement.getNotFound() != null && "ignore".equalsIgnoreCase( jaxbOneToManyElement.getNotFound().value() );
	}

	@Override
	public String getXmlNodeName() {
		return jaxbOneToManyElement.getNode();
	}
}
