/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
