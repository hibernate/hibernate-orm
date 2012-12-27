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
package org.hibernate.metamodel.internal.source.annotations;

import java.util.List;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.internal.source.annotations.attribute.AssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.ConfiguredClass;
import org.hibernate.metamodel.spi.source.AttributeSource;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class SourceHelper {
	/**
	 * Bind association attributes within {@param configuredClass} to the proper source impl based on its nature.
	 *
	 * @param configuredClass The holder of association attributes.
	 * @param attributeList Attribute source container, can't be <code>null</code>.
	 */
	public static void resolveAssociationAttributes(ConfiguredClass configuredClass, List<AttributeSource> attributeList) {
		for ( AssociationAttribute associationAttribute : configuredClass.getAssociationAttributes() ) {
			switch ( associationAttribute.getNature() ) {
				case ONE_TO_ONE:
				case MANY_TO_ONE: {
					attributeList.add( new ToOneAttributeSourceImpl( associationAttribute ) );
					break;
				}
				case MANY_TO_MANY:
				case ONE_TO_MANY:
				case ELEMENT_COLLECTION_BASIC:
				case ELEMENT_COLLECTION_EMBEDDABLE: {
					PluralAssociationAttribute pluralAssociationAttribute = (PluralAssociationAttribute) associationAttribute;
					AttributeSource source = pluralAssociationAttribute.isIndexed() ?
							new IndexedPluralAttributeSourceImpl( pluralAssociationAttribute, configuredClass )
							: new PluralAttributeSourceImpl( pluralAssociationAttribute, configuredClass );
					attributeList.add( source );
					break;
				}
				default: {
					throw new NotYetImplementedException();
				}
			}
		}
	}
}
