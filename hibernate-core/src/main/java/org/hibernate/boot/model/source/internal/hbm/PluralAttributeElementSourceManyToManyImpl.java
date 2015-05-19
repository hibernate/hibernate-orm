/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmFilterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmManyToManyCollectionElementType;
import org.hibernate.boot.model.source.spi.FetchCharacteristics;
import org.hibernate.boot.model.source.spi.FilterSource;
import org.hibernate.boot.model.source.spi.PluralAttributeElementNature;
import org.hibernate.boot.model.source.spi.PluralAttributeElementSourceManyToMany;
import org.hibernate.boot.model.source.spi.PluralAttributeSource;
import org.hibernate.boot.model.source.spi.RelationalValueSource;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class PluralAttributeElementSourceManyToManyImpl
		extends AbstractPluralAssociationElementSourceImpl
		implements PluralAttributeElementSourceManyToMany {
	private static final FilterSource[] NO_FILTER_SOURCES = new FilterSource[0];

	private final JaxbHbmManyToManyCollectionElementType jaxbManyToManyElement;
	private final String referencedEntityName;

	private final FetchCharacteristics fetchCharacteristics;

	private final List<RelationalValueSource> valueSources;
	private final FilterSource[] filterSources;

	public PluralAttributeElementSourceManyToManyImpl(
			MappingDocument mappingDocument,
			final PluralAttributeSource pluralAttributeSource,
			final JaxbHbmManyToManyCollectionElementType jaxbManyToManyElement) {
		super( mappingDocument, pluralAttributeSource );
		this.jaxbManyToManyElement = jaxbManyToManyElement;
		this.referencedEntityName = StringHelper.isNotEmpty( jaxbManyToManyElement.getEntityName() )
				? jaxbManyToManyElement.getEntityName()
				: mappingDocument.qualifyClassName( jaxbManyToManyElement.getClazz() );

		this.fetchCharacteristics = FetchCharacteristicsSingularAssociationImpl.interpretManyToManyElement(
				mappingDocument.getMappingDefaults(),
				jaxbManyToManyElement.getFetch(),
				jaxbManyToManyElement.getOuterJoin(),
				jaxbManyToManyElement.getLazy()
		);

		this.valueSources = RelationalValueSourceHelper.buildValueSources(
				sourceMappingDocument(),
				null,
				new RelationalValueSourceHelper.AbstractColumnsAndFormulasSource() {
					@Override
					public XmlElementMetadata getSourceType() {
						return XmlElementMetadata.MANY_TO_MANY;
					}

					@Override
					public String getSourceName() {
						return null;
					}

					@Override
					public String getFormulaAttribute() {
						return jaxbManyToManyElement.getFormulaAttribute();
					}

					@Override
					public String getColumnAttribute() {
						return jaxbManyToManyElement.getColumnAttribute();
					}

					@Override
					public List getColumnOrFormulaElements() {
						return jaxbManyToManyElement.getColumnOrFormula();
					}
				}
		);
		this.filterSources = buildFilterSources();
	}

	private FilterSource[] buildFilterSources() {
		final int size = jaxbManyToManyElement.getFilter().size();
		if ( size == 0 ) {
			return NO_FILTER_SOURCES;
		}

		FilterSource[] results = new FilterSource[size];
		for ( int i = 0; i < size; i++ ) {
			JaxbHbmFilterType element = jaxbManyToManyElement.getFilter().get( i );
			results[i] = new FilterSourceImpl( sourceMappingDocument(), element );
		}
		return results;
	}

	@Override
	public PluralAttributeElementNature getNature() {
		return PluralAttributeElementNature.MANY_TO_MANY;
	}

	@Override
	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	@Override
	public FilterSource[] getFilterSources() {
		return filterSources;
	}

	@Override
	public String getReferencedEntityAttributeName() {
		return jaxbManyToManyElement.getPropertyRef();
	}

	@Override
	public List<RelationalValueSource> getRelationalValueSources() {
		return valueSources;
	}

	@Override
	public boolean isIgnoreNotFound() {
		return jaxbManyToManyElement.getNotFound() != null && "ignore".equalsIgnoreCase( jaxbManyToManyElement.getNotFound().value() );
	}

	@Override
	public String getExplicitForeignKeyName() {
		return jaxbManyToManyElement.getForeignKey();
	}

	@Override
	public boolean isCascadeDeleteEnabled() {
		return false;
	}

	@Override
	public boolean isUnique() {
		return jaxbManyToManyElement.isUnique();
	}

	@Override
	public String getWhere() {
		return jaxbManyToManyElement.getWhere();
	}

	@Override
	public FetchCharacteristics getFetchCharacteristics() {
		return fetchCharacteristics;
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

	@Override
	public boolean isOrdered() {
		return StringHelper.isNotEmpty( getOrder() );
	}

	@Override
	public String getOrder() {
		return jaxbManyToManyElement.getOrderBy();
	}

	@Override
	public boolean createForeignKeyConstraint() {
		// TODO: Can HBM do something like JPA's @ForeignKey(NO_CONSTRAINT)?
		return true;
	}

}
