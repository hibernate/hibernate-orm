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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.jaxb.spi.hbm.EntityElement;
import org.hibernate.jaxb.spi.hbm.JaxbColumnElement;
import org.hibernate.jaxb.spi.hbm.JaxbJoinedSubclassElement;
import org.hibernate.jaxb.spi.hbm.JaxbKeyElement;
import org.hibernate.jaxb.spi.hbm.JaxbSubclassElement;
import org.hibernate.jaxb.spi.hbm.TableInformationSource;
import org.hibernate.metamodel.spi.source.ColumnSource;
import org.hibernate.metamodel.spi.source.EntitySource;
import org.hibernate.metamodel.spi.source.PrimaryKeyJoinColumnSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;
import org.hibernate.metamodel.spi.source.SubclassEntitySource;
import org.hibernate.metamodel.spi.source.TableSpecificationSource;

/**
 * @author Steve Ebersole
 */
public class SubclassEntitySourceImpl extends AbstractEntitySourceImpl implements SubclassEntitySource {
    private final EntitySource container;
	private final TableSpecificationSource primaryTable;
	private final boolean isJoinedSubclass;
	private final JaxbKeyElement key;
	private final List<PrimaryKeyJoinColumnSource> primaryKeyJoinColumnSources;
	protected SubclassEntitySourceImpl(
			MappingDocument sourceMappingDocument,
			EntityElement entityElement,
			EntitySource container) {
		super( sourceMappingDocument, entityElement );
		this.container = container;
		this.primaryTable = TableInformationSource.class.isInstance( entityElement )
				? Helper.createTableSource( sourceMappingDocument(), (TableInformationSource) entityElement, this )
				: null;
		this.isJoinedSubclass = JaxbJoinedSubclassElement.class.isInstance( entityElement );
		this.key = isJoinedSubclass? ( (JaxbJoinedSubclassElement) entityElement() ).getKey() : null;
		if ( isJoinedSubclass ) {
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
			this.primaryKeyJoinColumnSources = new ArrayList<PrimaryKeyJoinColumnSource>( valueSources.size() );
			for(final RelationalValueSource valueSource : valueSources){
				 primaryKeyJoinColumnSources.add( new PrimaryKeyJoinColumnSourceImpl( ColumnSource.class.cast( valueSource ) ) );
			}
		} else {
			this.primaryKeyJoinColumnSources = null;
		}

		afterInstantiation();
	}

	@Override
	public TableSpecificationSource getPrimaryTable() {
		return primaryTable;
	}

	@Override
	public String getDiscriminatorMatchValue() {
		return JaxbSubclassElement.class.isInstance( entityElement() )
				? ( (JaxbSubclassElement) entityElement() ).getDiscriminatorValue()
				: null;
	}

	@Override
	public EntitySource superclassEntitySource() {
	    return container;
	}

	@Override
	public String getJoinedForeignKeyName() {
		if ( isJoinedSubclass ) {
			return key.getForeignKey();
		}
		return null;
	}

	@Override
	public List<PrimaryKeyJoinColumnSource> getPrimaryKeyJoinColumnSources() {
		return primaryKeyJoinColumnSources;
	}
}
