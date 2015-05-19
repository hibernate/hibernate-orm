/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.List;
import java.util.Locale;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFetchStyleEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmOnDeleteEnum;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSecondaryTableType;
import org.hibernate.boot.model.CustomSql;
import org.hibernate.boot.model.source.spi.ColumnSource;
import org.hibernate.boot.model.source.spi.EntityNamingSource;
import org.hibernate.boot.model.source.spi.InLineViewSource;
import org.hibernate.boot.model.source.spi.SecondaryTableSource;
import org.hibernate.boot.model.source.spi.TableSource;
import org.hibernate.boot.model.source.spi.TableSpecificationSource;
import org.hibernate.engine.FetchStyle;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 */
class SecondaryTableSourceImpl extends AbstractHbmSourceNode implements SecondaryTableSource {
	private final JaxbHbmSecondaryTableType jaxbSecondaryTableMapping;
	private final TableSpecificationSource joinTable;
	private final String logicalTableName;
	private final List<ColumnSource> keyColumnSources;

	@SuppressWarnings("unchecked")
	public SecondaryTableSourceImpl(
			MappingDocument sourceMappingDocument,
			final JaxbHbmSecondaryTableType jaxbSecondaryTableMapping,
			EntityNamingSource entityNamingSource,
			Helper.InLineViewNameInferrer inLineViewNameInferrer) {
		super( sourceMappingDocument );
		this.jaxbSecondaryTableMapping = jaxbSecondaryTableMapping;
		this.joinTable = Helper.createTableSource(
				sourceMappingDocument,
				jaxbSecondaryTableMapping,
				inLineViewNameInferrer
		);

		if ( joinTable instanceof TableSource ) {
			if ( StringHelper.isEmpty( ( (TableSource) joinTable ).getExplicitTableName() ) ) {
				throw new MappingException(
						String.format(
								Locale.ENGLISH,
								"Secondary table (<join/>) must explicitly name table or sub-select, but neither " +
										"specified for entity [%s]",
								entityNamingSource.getEntityName()
						),
						sourceMappingDocument.getOrigin()
				);
			}
		}

		this.logicalTableName = joinTable instanceof TableSource
					? ( (TableSource) joinTable ).getExplicitTableName()
					: ( (InLineViewSource) joinTable ).getLogicalName();

		this.keyColumnSources = RelationalValueSourceHelper.buildColumnSources(
				sourceMappingDocument,
				logicalTableName,
				new RelationalValueSourceHelper.AbstractColumnsAndFormulasSource() {
					@Override
					public XmlElementMetadata getSourceType() {
						return XmlElementMetadata.KEY;
					}

					@Override
					public String getSourceName() {
						return null;
					}

					@Override
					public String getColumnAttribute() {
						return jaxbSecondaryTableMapping.getKey().getColumnAttribute();
					}

					@Override
					public List getColumnOrFormulaElements() {
						return jaxbSecondaryTableMapping.getKey().getColumn();
					}

					@Override
					public Boolean isNullable() {
						return false;
					}
				}
		);
	}

	@Override
	public TableSpecificationSource getTableSource() {
		return joinTable;
	}

	@Override
	public List<ColumnSource> getPrimaryKeyColumnSources() {
		return keyColumnSources;
	}

	@Override
	public String getLogicalTableNameForContainedColumns() {
		return logicalTableName;
	}

	@Override
	public String getComment() {
		return jaxbSecondaryTableMapping.getComment();
	}

	@Override
	public FetchStyle getFetchStyle() {
		return jaxbSecondaryTableMapping.getFetch() == JaxbHbmFetchStyleEnum.JOIN
				? FetchStyle.JOIN
				: FetchStyle.SELECT;
	}

	@Override
	public boolean isInverse() {
		return jaxbSecondaryTableMapping.isInverse();
	}

	@Override
	public boolean isOptional() {
		return jaxbSecondaryTableMapping.isOptional();
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return jaxbSecondaryTableMapping.getKey().getOnDelete() == JaxbHbmOnDeleteEnum.CASCADE;
	}

	@Override
	public String getExplicitForeignKeyName() {
		return jaxbSecondaryTableMapping.getKey().getForeignKey();
	}
	
	@Override
	public boolean createForeignKeyConstraint() {
		// TODO: Can HBM do something like JPA's @ForeignKey(NO_CONSTRAINT)?
		return true;
	}

	@Override
	public CustomSql getCustomSqlInsert() {
		return Helper.buildCustomSql( jaxbSecondaryTableMapping.getSqlInsert() );
	}

	@Override
	public CustomSql getCustomSqlUpdate() {
		return Helper.buildCustomSql( jaxbSecondaryTableMapping.getSqlUpdate() );
	}

	@Override
	public CustomSql getCustomSqlDelete() {
		return Helper.buildCustomSql( jaxbSecondaryTableMapping.getSqlDelete() );
	}
}
