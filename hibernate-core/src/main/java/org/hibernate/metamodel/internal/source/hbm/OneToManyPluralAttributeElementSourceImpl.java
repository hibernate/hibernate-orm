/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.hbm;

import java.util.Set;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jaxb.spi.hbm.JaxbOneToManyElement;
import org.hibernate.metamodel.spi.source.OneToManyPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;

/**
 * @author Steve Ebersole
 */
public class OneToManyPluralAttributeElementSourceImpl
		extends AbstractPluralAssociationElementSourceImpl
		implements OneToManyPluralAttributeElementSource {
	private final JaxbOneToManyElement oneToManyElement;
	private final Set<CascadeStyle> cascadeStyles;

	public OneToManyPluralAttributeElementSourceImpl(
			MappingDocument mappingDocument,
			final PluralAttributeSource pluralAttributeSource,
			final JaxbOneToManyElement oneToManyElement,
			String cascadeString) {
		super( mappingDocument, pluralAttributeSource );
		this.oneToManyElement = oneToManyElement;
		this.cascadeStyles = Helper.interpretCascadeStyles( cascadeString, bindingContext() );
	}

	@Override
	public Nature getNature() {
		return Nature.ONE_TO_MANY;
	}

	@Override
	public String getReferencedEntityName() {
		return StringHelper.isNotEmpty( oneToManyElement.getEntityName() )
				? oneToManyElement.getEntityName()
				: bindingContext().qualifyClassName( oneToManyElement.getClazz() );
	}

	@Override
	public boolean isNotFoundAnException() {
		return oneToManyElement.getNotFound() == null
				|| ! "ignore".equals( oneToManyElement.getNotFound().value() );
	}

	@Override
	public Set<CascadeStyle> getCascadeStyles() {
		return cascadeStyles;
	}
}
