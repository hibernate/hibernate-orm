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

import java.util.List;

import org.hibernate.jaxb.spi.hbm.JaxbColumnElement;
import org.hibernate.jaxb.spi.hbm.JaxbKeyElement;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.AttributeSourceContainer;
import org.hibernate.metamodel.spi.source.PluralAttributeKeySource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeKeySourceImpl
		extends AbstractHbmSourceNode
		implements PluralAttributeKeySource {
	private final JaxbKeyElement keyElement;

	private final List<RelationalValueSource> valueSources;

	public PluralAttributeKeySourceImpl(
			MappingDocument mappingDocument,
			final JaxbKeyElement keyElement,
			final AttributeSourceContainer container) {
		super( mappingDocument );
		this.keyElement = keyElement;

		this.valueSources = Helper.buildValueSources(
				sourceMappingDocument(),
				new Helper.ValueSourcesAdapter() {
					@Override
					public boolean isIncludedInInsertByDefault() {
						return true;
					}

					@Override
					public boolean isIncludedInUpdateByDefault() {
						return Helper.getValue( keyElement.isUpdate(), true );
					}

					@Override
					public String getColumnAttribute() {
						return keyElement.getColumnAttribute();
					}

					@Override
					public List<JaxbColumnElement> getColumn() {
						return keyElement.getColumn();
					}

					@Override
					public boolean isForceNotNull() {
						return Helper.getValue( keyElement.isNotNull(), false );
					}
				}
		);
	}

	@Override
	public String getExplicitForeignKeyName() {
		return keyElement.getForeignKey();
	}

	@Override
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {
		return keyElement.getPropertyRef() == null
				? null
				: new JoinColumnResolutionDelegate() {
			@Override
			public List<? extends Value> getJoinColumns(JoinColumnResolutionContext context) {
				return context.resolveRelationalValuesForAttribute( keyElement.getPropertyRef() );
			}

			@Override
			public String getReferencedAttributeName() {
				return keyElement.getPropertyRef();
			}

			@Override
			public TableSpecification getReferencedTable(JoinColumnResolutionContext context) {
				return context.resolveTableForAttribute( keyElement.getPropertyRef() );
			}
		};
	}

	@Override
	public ForeignKey.ReferentialAction getOnDeleteAction() {
		return "cascade".equals( keyElement.getOnDelete().value() )
				? ForeignKey.ReferentialAction.CASCADE
				: ForeignKey.ReferentialAction.NO_ACTION;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return valueSources;
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		return true;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		return true;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		return true;
	}
}
