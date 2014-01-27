/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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

import java.util.Map;

import org.hibernate.metamodel.internal.source.annotations.attribute.PluralAssociationAttribute;
import org.hibernate.metamodel.spi.source.HibernateTypeSource;
import org.hibernate.metamodel.spi.source.PluralAttributeIndexSource;

/**
 * @author Gail Badner
 */
public abstract class AbstractPluralAttributeIndexSourceImpl implements PluralAttributeIndexSource {
	private final PluralAssociationAttribute attribute;

	public AbstractPluralAttributeIndexSourceImpl(PluralAssociationAttribute attribute) {
		this.attribute = attribute;
	}

	@Override
	public HibernateTypeSource getTypeInformation() {
		return new HibernateTypeSource() {
			@Override
			public String getName() {
				return attribute.getIndexTypeResolver().getExplicitHibernateTypeName();
			}

			@Override
			public Map<String, String> getParameters() {
				return attribute.getIndexTypeResolver().getExplicitHibernateTypeParameters();
			}
			@Override
			public Class getJavaType() {
				return null;
			}
		};
	}

	@Override
	public boolean isReferencedEntityAttribute() {
		return false;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return false;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return false;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return false;
	}

	protected PluralAssociationAttribute pluralAssociationAttribute() {
		return attribute;
	}
}
