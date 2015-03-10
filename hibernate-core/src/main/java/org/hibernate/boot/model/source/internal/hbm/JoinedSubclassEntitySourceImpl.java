/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.model.source.internal.hbm;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmJoinedSubclassEntityType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmKeyType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOnDeleteEnum;
import org.hibernate.boot.model.source.spi.ColumnSource;
import org.hibernate.boot.model.source.spi.EntitySource;
import org.hibernate.boot.model.source.spi.JoinedSubclassEntitySource;
import org.hibernate.boot.model.source.spi.RelationalValueSource;

/**
 * @author Strong Liu
 * @author Steve Ebersole
 */
public class JoinedSubclassEntitySourceImpl extends SubclassEntitySourceImpl implements JoinedSubclassEntitySource {
	private final JaxbHbmKeyType jaxbKeyMapping;
	private final List<ColumnSource> primaryKeyJoinColumnSources;

	public JoinedSubclassEntitySourceImpl(
			MappingDocument sourceMappingDocument,
			JaxbHbmJoinedSubclassEntityType jaxbJoinedSubclassMapping,
			EntitySource container) {
		super( sourceMappingDocument, jaxbJoinedSubclassMapping, container );
		this.jaxbKeyMapping = jaxbJoinedSubclassMapping.getKey();
		List<RelationalValueSource> valueSources = RelationalValueSourceHelper.buildValueSources(
				sourceMappingDocument(),
				null,
				new RelationalValueSourceHelper.AbstractColumnsAndFormulasSource() {
					@Override
					public XmlElementMetadata getSourceType() {
						return null;
					}

					@Override
					public String getSourceName() {
						return null;
					}

					@Override
					public String getColumnAttribute() {
						return jaxbKeyMapping.getColumnAttribute();
					}

					@Override
					public List getColumnOrFormulaElements() {
						return jaxbKeyMapping.getColumn();
					}

					@Override
					public Boolean isNullable() {
						return false;
					}
				}
		);

		this.primaryKeyJoinColumnSources = new ArrayList<ColumnSource>( valueSources.size() );
		for ( RelationalValueSource valueSource : valueSources ) {
			primaryKeyJoinColumnSources.add( ColumnSource.class.cast( valueSource ) ) ;
		}
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return jaxbKeyMapping.getOnDelete() == JaxbHbmOnDeleteEnum.CASCADE;
	}

	@Override
	public String getExplicitForeignKeyName() {
		return jaxbKeyMapping.getForeignKey();
	}
	
	@Override
	public boolean createForeignKeyConstraint() {
		// TODO: Can HBM do something like JPA's @ForeignKey(NO_CONSTRAINT)?
		return true;
	}

	@Override
	public List<ColumnSource> getPrimaryKeyColumnSources() {
		return primaryKeyJoinColumnSources;
	}
}
