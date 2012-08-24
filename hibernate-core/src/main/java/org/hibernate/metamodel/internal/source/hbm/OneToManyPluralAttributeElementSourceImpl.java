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

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jaxb.spi.hbm.JaxbOneToManyElement;
import org.hibernate.jaxb.spi.hbm.PluralAttributeElement;
import org.hibernate.metamodel.spi.source.OneToManyPluralAttributeElementSource;

/**
 * @author Steve Ebersole
 */
public class OneToManyPluralAttributeElementSourceImpl
		extends AbstractHbmSourceNode
		implements OneToManyPluralAttributeElementSource {
	private final PluralAttributeElement pluralAttributeElement;
	private final JaxbOneToManyElement oneToManyElement;

	public OneToManyPluralAttributeElementSourceImpl(
			MappingDocument mappingDocument,
			PluralAttributeElement pluralAttributeElement,
			JaxbOneToManyElement oneToManyElement) {
		super( mappingDocument );
		this.pluralAttributeElement = pluralAttributeElement;
		this.oneToManyElement = oneToManyElement;
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
	public Iterable<CascadeStyle> getCascadeStyles() {
		return Helper.interpretCascadeStyles( pluralAttributeElement.getCascade(), bindingContext() );
	}
}
