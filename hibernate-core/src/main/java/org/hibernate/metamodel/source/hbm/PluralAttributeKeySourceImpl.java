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
package org.hibernate.metamodel.source.hbm;

import java.util.List;

import org.hibernate.internal.jaxb.mapping.hbm.JaxbKeyElement;
import org.hibernate.metamodel.relational.ForeignKey;
import org.hibernate.metamodel.source.binder.AttributeSourceContainer;
import org.hibernate.metamodel.source.binder.PluralAttributeKeySource;
import org.hibernate.metamodel.source.binder.RelationalValueSource;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeKeySourceImpl implements PluralAttributeKeySource {
	private final JaxbKeyElement keyElement;

	private final List<RelationalValueSource> valueSources;

	public PluralAttributeKeySourceImpl(
			final JaxbKeyElement keyElement,
			final AttributeSourceContainer container) {
		this.keyElement = keyElement;

		this.valueSources = Helper.buildValueSources(
				new Helper.ValueSourcesAdapter() {
					@Override
					public String getContainingTableName() {
						return null;
					}

					@Override
					public boolean isIncludedInInsertByDefault() {
						return true;
					}

					@Override
					public boolean isIncludedInUpdateByDefault() {
						return Helper.getBooleanValue( keyElement.isUpdate(), true );
					}

					@Override
					public String getColumnAttribute() {
						return keyElement.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return null;
					}

					@Override
					public List getColumnOrFormulaElements() {
						return keyElement.getColumn();
					}
				},
				container.getLocalBindingContext()
		);
	}

	@Override
	public List<RelationalValueSource> getValueSources() {
		return valueSources;
	}

	@Override
	public String getExplicitForeignKeyName() {
		return keyElement.getForeignKey();
	}

	@Override
	public ForeignKey.ReferentialAction getOnDeleteAction() {
		return "cascade".equals( keyElement.getOnDelete() )
				? ForeignKey.ReferentialAction.CASCADE
				: ForeignKey.ReferentialAction.NO_ACTION;
	}

	@Override
	public String getReferencedEntityAttributeName() {
		return keyElement.getPropertyRef();
	}
}
