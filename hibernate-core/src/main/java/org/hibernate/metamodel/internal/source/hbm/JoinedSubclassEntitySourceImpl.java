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
package org.hibernate.metamodel.internal.source.hbm;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.jaxb.spi.hbm.JaxbColumnElement;
import org.hibernate.jaxb.spi.hbm.JaxbJoinedSubclassElement;
import org.hibernate.jaxb.spi.hbm.JaxbKeyElement;
import org.hibernate.jaxb.spi.hbm.JaxbOnDeleteAttribute;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.ColumnSource;
import org.hibernate.metamodel.spi.source.EntitySource;
import org.hibernate.metamodel.spi.source.JoinedSubclassEntitySource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class JoinedSubclassEntitySourceImpl extends SubclassEntitySourceImpl implements JoinedSubclassEntitySource {
	private final JaxbKeyElement key;
	private final List<ColumnSource> primaryKeyJoinColumnSources;

	public JoinedSubclassEntitySourceImpl(MappingDocument sourceMappingDocument, JaxbJoinedSubclassElement entityElement, EntitySource container) {
		super( sourceMappingDocument, entityElement, container );
		this.key = entityElement.getKey();
		List<RelationalValueSource> valueSources = Helper.buildValueSources(
				sourceMappingDocument(),
				new Helper.ValueSourcesAdapter() {
					@Override
					public boolean isIncludedInInsertByDefault() {
						return true;
					}

					@Override
					public boolean isIncludedInUpdateByDefault() {
						return Helper.getValue( key.isUpdate(), true );
					}

					@Override
					public String getColumnAttribute() {
						return key.getColumnAttribute();
					}

					@Override
					public List<JaxbColumnElement> getColumn() {
						return key.getColumn();
					}

					@Override
					public boolean isForceNotNull() {
						return Helper.getValue( key.isNotNull(), false );
					}


				}
		);
		this.primaryKeyJoinColumnSources = new ArrayList<ColumnSource>( valueSources.size() );
		for ( final RelationalValueSource valueSource : valueSources ) {
			primaryKeyJoinColumnSources.add( ColumnSource.class.cast( valueSource ) ) ;
		}
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return key.getOnDelete() == JaxbOnDeleteAttribute.CASCADE;
	}

	@Override
	public String getExplicitForeignKeyName() {
		return key.getForeignKey();
	}

	@Override
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {
		return key.getPropertyRef() == null
				? null
				: new JoinColumnResolutionDelegate() {
			@Override
			public List<? extends Value> getJoinColumns(JoinColumnResolutionContext context) {
				return context.resolveRelationalValuesForAttribute( key.getPropertyRef() );
			}

			@Override
			public TableSpecification getReferencedTable(JoinColumnResolutionContext context) {
				return context.resolveTableForAttribute( key.getPropertyRef() );
			}

			@Override
			public String getReferencedAttributeName() {
				return key.getPropertyRef();
			}
		};
	}


	@Override
	public List<ColumnSource> getPrimaryKeyColumnSources() {
		return primaryKeyJoinColumnSources;
	}
}
