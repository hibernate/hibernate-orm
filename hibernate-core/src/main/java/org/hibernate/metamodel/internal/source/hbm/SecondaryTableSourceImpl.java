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

import org.hibernate.internal.jaxb.mapping.hbm.JaxbColumnElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbJoinElement;
import org.hibernate.metamodel.spi.source.InLineViewSource;
import org.hibernate.metamodel.spi.source.PrimaryKeyJoinColumnSource;
import org.hibernate.metamodel.spi.source.SecondaryTableSource;
import org.hibernate.metamodel.spi.source.TableSource;
import org.hibernate.metamodel.spi.source.TableSpecificationSource;

/**
 * @author Steve Ebersole
 */
class SecondaryTableSourceImpl extends AbstractHbmSourceNode implements SecondaryTableSource {
	private final JaxbJoinElement joinElement;
	private final TableSpecificationSource joinTable;
	private final List<PrimaryKeyJoinColumnSource> joinColumns;

	public SecondaryTableSourceImpl(
			MappingDocument sourceMappingDocument,
			JaxbJoinElement joinElement,
			Helper.InLineViewNameInferrer inLineViewNameInferrer) {
		super( sourceMappingDocument );
		this.joinElement = joinElement;
		this.joinTable = Helper.createTableSource( sourceMappingDocument(), joinElement, inLineViewNameInferrer );

		joinColumns = new ArrayList<PrimaryKeyJoinColumnSource>();
		if ( joinElement.getKey().getColumnAttribute() != null ) {
			if ( joinElement.getKey().getColumn().size() > 0 ) {
				throw makeMappingException( "<join/> defined both column attribute and nested <column/>" );
			}
			joinColumns.add(
					new PrimaryKeyJoinColumnSource() {
						@Override
						public String getColumnName() {
							return SecondaryTableSourceImpl.this.joinElement.getKey().getColumnAttribute();
						}

						@Override
						public String getReferencedColumnName() {
							return null;
						}

						@Override
						public String getColumnDefinition() {
							return null;
						}
					}
			);
		}
		for ( final JaxbColumnElement columnElement : joinElement.getKey().getColumn() ) {
			joinColumns.add(
					new PrimaryKeyJoinColumnSource() {
						@Override
						public String getColumnName() {
							return columnElement.getName();
						}

						@Override
						public String getReferencedColumnName() {
							return null;
						}

						@Override
						public String getColumnDefinition() {
							return columnElement.getSqlType();
						}
					}
			);
		}
	}

	@Override
	public TableSpecificationSource getTableSource() {
		return joinTable;
	}

	@Override
	public String getForeignKeyName() {
		return joinElement.getKey().getForeignKey();
	}

	@Override
	public List<PrimaryKeyJoinColumnSource> getJoinColumns() {
		return joinColumns;
	}

	public String getLogicalTableNameForContainedColumns() {
		return TableSource.class.isInstance( joinTable )
				? ( (TableSource) joinTable ).getExplicitTableName()
				: ( (InLineViewSource) joinTable ).getLogicalName();
	}
}
