/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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

import org.hibernate.engine.FetchStyle;
import org.hibernate.jaxb.spi.hbm.JaxbColumnElement;
import org.hibernate.jaxb.spi.hbm.JaxbFetchStyleAttribute;
import org.hibernate.jaxb.spi.hbm.JaxbJoinElement;
import org.hibernate.jaxb.spi.hbm.JaxbOnDeleteAttribute;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.ColumnSource;
import org.hibernate.metamodel.spi.source.InLineViewSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.hibernate.metamodel.spi.source.SecondaryTableSource;
import org.hibernate.metamodel.spi.source.TableSource;
import org.hibernate.metamodel.spi.source.TableSpecificationSource;

/**
 * @author Steve Ebersole
 */
class SecondaryTableSourceImpl extends AbstractHbmSourceNode implements SecondaryTableSource {
	private final JaxbJoinElement joinElement;
	private final TableSpecificationSource joinTable;
	private final List<ColumnSource> columnSources;
	private final JoinColumnResolutionDelegate fkJoinColumnResolutionDelegate;

	@SuppressWarnings("unchecked")
	public SecondaryTableSourceImpl(
			MappingDocument sourceMappingDocument,
			final JaxbJoinElement joinElement,
			Helper.InLineViewNameInferrer inLineViewNameInferrer) {
		super( sourceMappingDocument );
		this.joinElement = joinElement;
		this.joinTable = Helper.createTableSource( sourceMappingDocument(), joinElement, inLineViewNameInferrer );

		// the cast is ok here because the adapter should never be returning formulas since the schema does not allow it
		this.columnSources = extractColumnSources();

		fkJoinColumnResolutionDelegate = joinElement.getKey().getPropertyRef() == null
				? null
				: new JoinColumnResolutionDelegateImpl( joinElement );
	}

	private List<ColumnSource> extractColumnSources() {
		final List<ColumnSource> columnSources = new ArrayList<ColumnSource>();
		final List<RelationalValueSource> valueSources = Helper.buildValueSources(
				sourceMappingDocument(),
				new Helper.ValueSourcesAdapter() {
					@Override
					public String getContainingTableName() {
						return joinElement.getTable();
					}

					@Override
					public String getColumnAttribute() {
						return joinElement.getKey().getColumnAttribute();
					}

					@Override
					public List<JaxbColumnElement> getColumn() {
						return joinElement.getKey().getColumn();
					}

					@Override
					public boolean isForceNotNull() {
						return true;
					}
				}
		);
		for ( RelationalValueSource valueSource : valueSources ) {
			columnSources.add( (ColumnSource) valueSource );
		}
		return columnSources;
	}

	@Override
	public TableSpecificationSource getTableSource() {
		return joinTable;
	}

	@Override
	public List<ColumnSource> getPrimaryKeyColumnSources() {
		return columnSources;
	}

	@Override
	public String getComment() {
		return joinElement.getComment();
	}

	@Override
	public FetchStyle getFetchStyle() {
		return joinElement.getFetch() == JaxbFetchStyleAttribute.JOIN ?
				FetchStyle.JOIN :
				FetchStyle.SELECT;
	}

	@Override
	public boolean isInverse() {
		return joinElement.isInverse();
	}

	@Override
	public boolean isOptional() {
		return joinElement.isOptional();
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return joinElement.getKey().getOnDelete() == JaxbOnDeleteAttribute.CASCADE;
	}

	@Override
	public String getExplicitForeignKeyName() {
		return joinElement.getKey().getForeignKey();
	}

	@Override
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {
		return fkJoinColumnResolutionDelegate;
	}

	@Override
	public CustomSQL getCustomSqlInsert() {
		return Helper.buildCustomSql( joinElement.getSqlInsert() );
	}

	@Override
	public CustomSQL getCustomSqlUpdate() {
		return Helper.buildCustomSql( joinElement.getSqlUpdate() );
	}

	@Override
	public CustomSQL getCustomSqlDelete() {
		return Helper.buildCustomSql( joinElement.getSqlDelete() );
	}


	public String getLogicalTableNameForContainedColumns() {
		return TableSource.class.isInstance( joinTable )
				? ( (TableSource) joinTable ).getExplicitTableName()
				: ( (InLineViewSource) joinTable ).getLogicalName();
	}

	private static class JoinColumnResolutionDelegateImpl implements JoinColumnResolutionDelegate {
		private final JaxbJoinElement joinElement;

		public JoinColumnResolutionDelegateImpl(JaxbJoinElement joinElement) {
			this.joinElement = joinElement;
		}

		@Override
		public List<? extends Value> getJoinColumns(JoinColumnResolutionContext context) {
			return context.resolveRelationalValuesForAttribute( getReferencedAttributeName() );
		}

		@Override
		public String getReferencedAttributeName() {
			return joinElement.getKey().getPropertyRef();
		}

		@Override
		public TableSpecification getReferencedTable(JoinColumnResolutionContext context) {
			return context.resolveTableForAttribute( joinElement.getKey().getPropertyRef() );
		}
	}
}
