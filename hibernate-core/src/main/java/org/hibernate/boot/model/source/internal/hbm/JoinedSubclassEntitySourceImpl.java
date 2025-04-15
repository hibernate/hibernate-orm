/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

		this.primaryKeyJoinColumnSources = new ArrayList<>( valueSources.size() );
		for ( RelationalValueSource valueSource : valueSources ) {
			primaryKeyJoinColumnSources.add( (ColumnSource) valueSource ) ;
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

	@Override
	public String getDiscriminatorMatchValue() {
		return jaxbEntityMapping() instanceof JaxbHbmJoinedSubclassEntityType joinedSubclassEntityType
				? joinedSubclassEntityType.getDiscriminatorValue()
				: null;
	}
}
