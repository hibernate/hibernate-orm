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

import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIndexManyToManyType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmMapKeyManyToManyType;
import org.hibernate.boot.model.source.spi.HibernateTypeSource;
import org.hibernate.boot.model.source.spi.PluralAttributeIndexNature;
import org.hibernate.boot.model.source.spi.PluralAttributeMapKeyManyToManySource;
import org.hibernate.boot.model.source.spi.RelationalValueSource;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeMapKeyManyToManySourceImpl
		implements PluralAttributeMapKeyManyToManySource {

	private final String referencedEntityName;
	private final String fkName;

	private final HibernateTypeSource hibernateTypeSource;
	private final List<RelationalValueSource> relationalValueSources;

	public PluralAttributeMapKeyManyToManySourceImpl(
			MappingDocument mappingDocument,
			PluralAttributeSourceMapImpl pluralAttributeSourceMap,
			final JaxbHbmMapKeyManyToManyType jaxbMapKeyManyToManyMapping) {
		this.referencedEntityName = StringHelper.isNotEmpty( jaxbMapKeyManyToManyMapping.getEntityName() )
				? jaxbMapKeyManyToManyMapping.getEntityName()
				: mappingDocument.qualifyClassName( jaxbMapKeyManyToManyMapping.getClazz() );
		this.fkName = jaxbMapKeyManyToManyMapping.getForeignKey();

		this.hibernateTypeSource = new HibernateTypeSourceImpl(
				jaxbMapKeyManyToManyMapping.getEntityName()
		);

		this.relationalValueSources = RelationalValueSourceHelper.buildValueSources(
				mappingDocument,
				null,
				new RelationalValueSourceHelper.AbstractColumnsAndFormulasSource() {
					@Override
					public XmlElementMetadata getSourceType() {
						return XmlElementMetadata.MAP_KEY_MANY_TO_MANY;
					}

					@Override
					public String getSourceName() {
						return null;
					}

					@Override
					public String getFormulaAttribute() {
						return jaxbMapKeyManyToManyMapping.getFormulaAttribute();
					}

					@Override
					public String getColumnAttribute() {
						return jaxbMapKeyManyToManyMapping.getColumnAttribute();
					}

					@Override
					public List getColumnOrFormulaElements() {
						return jaxbMapKeyManyToManyMapping.getColumnOrFormula();
					}
				}
		);
	}

	public PluralAttributeMapKeyManyToManySourceImpl(
			MappingDocument mappingDocument,
			PluralAttributeSourceMapImpl pluralAttributeSourceMap,
			final JaxbHbmIndexManyToManyType jaxbIndexManyToManyMapping) {
		this.referencedEntityName = StringHelper.isNotEmpty( jaxbIndexManyToManyMapping.getEntityName() )
				? jaxbIndexManyToManyMapping.getEntityName()
				: mappingDocument.qualifyClassName( jaxbIndexManyToManyMapping.getClazz() );
		this.fkName = jaxbIndexManyToManyMapping.getForeignKey();

		this.hibernateTypeSource = new HibernateTypeSourceImpl(
				jaxbIndexManyToManyMapping.getEntityName()
		);

		this.relationalValueSources = RelationalValueSourceHelper.buildValueSources(
				mappingDocument,
				null,
				new RelationalValueSourceHelper.AbstractColumnsAndFormulasSource() {
					@Override
					public XmlElementMetadata getSourceType() {
						return XmlElementMetadata.INDEX_MANY_TO_MANY;
					}

					@Override
					public String getSourceName() {
						return null;
					}

					@Override
					public String getColumnAttribute() {
						return jaxbIndexManyToManyMapping.getColumnAttribute();
					}

					@Override
					public List getColumnOrFormulaElements() {
						return jaxbIndexManyToManyMapping.getColumn();
					}
				}
		);
	}

	@Override
	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	@Override
	public String getExplicitForeignKeyName() {
		return fkName;
	}

	@Override
	public PluralAttributeIndexNature getNature() {
		return PluralAttributeIndexNature.MANY_TO_MANY;
	}

	@Override
	public HibernateTypeSource getTypeInformation() {
		return hibernateTypeSource;
	}

	@Override
	public String getXmlNodeName() {
		return null;
	}

	@Override
	public Nature getMapKeyNature() {
		return Nature.MANY_TO_MANY;
	}

	@Override
	public boolean isReferencedEntityAttribute() {
		return true;
	}

	@Override
	public List<RelationalValueSource> getRelationalValueSources() {
		return relationalValueSources;
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
		return false;
	}
}
