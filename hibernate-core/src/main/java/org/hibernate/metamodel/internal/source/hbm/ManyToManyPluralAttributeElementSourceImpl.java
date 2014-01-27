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
import java.util.Set;

import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jaxb.spi.hbm.JaxbColumnElement;
import org.hibernate.jaxb.spi.hbm.JaxbFilterElement;
import org.hibernate.jaxb.spi.hbm.JaxbManyToManyElement;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.metamodel.spi.source.FilterSource;
import org.hibernate.metamodel.spi.source.ManyToManyPluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;
import org.hibernate.metamodel.spi.source.RelationalValueSource;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class ManyToManyPluralAttributeElementSourceImpl
		extends AbstractPluralAssociationElementSourceImpl
		implements ManyToManyPluralAttributeElementSource {
	private final JaxbManyToManyElement manyToManyElement;
	private final Set<CascadeStyle> cascadeStyles;

	private final List<RelationalValueSource> valueSources;
	private final FilterSource[] filterSources;
	public ManyToManyPluralAttributeElementSourceImpl(
			MappingDocument mappingDocument,
			final PluralAttributeSource pluralAttributeSource,
			final JaxbManyToManyElement manyToManyElement,
			String cascadeString) {
		super( mappingDocument, pluralAttributeSource );
		this.manyToManyElement = manyToManyElement;
		this.cascadeStyles = Helper.interpretCascadeStyles( cascadeString, bindingContext() );

		this.valueSources = Helper.buildValueSources(
				sourceMappingDocument(),
				new Helper.ValueSourcesAdapter() {
					@Override
					public boolean isIncludedInInsertByDefault() {
						return true;
					}

					@Override
					public boolean isIncludedInUpdateByDefault() {
						return true;
					}

					@Override
					public String getColumnAttribute() {
						return manyToManyElement.getColumnAttribute();
					}

					@Override
					public String getFormulaAttribute() {
						return manyToManyElement.getFormulaAttribute();
					}

					@Override
					public List<JaxbColumnElement> getColumn() {
						return manyToManyElement.getColumn();
					}

					@Override
					public List<String> getFormula() {
						return manyToManyElement.getFormula();
					}
				}
		);
		this.filterSources = buildFilterSources();
	}

	private FilterSource[] buildFilterSources() {
			final int size = manyToManyElement.getFilter().size();
			if ( size == 0 ) {
				return null;
			}

			FilterSource[] results = new FilterSource[size];
			for ( int i = 0; i < size; i++ ) {
				JaxbFilterElement element = manyToManyElement.getFilter().get( i );
				results[i] = new FilterSourceImpl( sourceMappingDocument(), element );
			}
			return results;
	}

	@Override
	public Nature getNature() {
		return Nature.MANY_TO_MANY;
	}

	@Override
	public String getReferencedEntityName() {
		return StringHelper.isNotEmpty( manyToManyElement.getEntityName() )
				? manyToManyElement.getEntityName()
				: bindingContext().qualifyClassName( manyToManyElement.getClazz() );
	}

	@Override
	public FilterSource[] getFilterSources() {
		return filterSources;
	}

	@Override
	public String getReferencedEntityAttributeName() {
		return manyToManyElement.getPropertyRef();
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return valueSources;
	}

	@Override
	public boolean isNotFoundAnException() {
		return manyToManyElement.getNotFound() == null || !"ignore".equals( manyToManyElement.getNotFound().value() );
	}

	@Override
	public String getExplicitForeignKeyName() {
		return manyToManyElement.getForeignKey();
	}

	@Override
	public JoinColumnResolutionDelegate getForeignKeyTargetColumnResolutionDelegate() {
		return manyToManyElement.getPropertyRef() == null
				? null
				: new JoinColumnResolutionDelegateImpl();
	}

	@Override
	public boolean isUnique() {
		return manyToManyElement.isUnique();
	}

	@Override
	public String getWhere() {
		return manyToManyElement.getWhere();
	}

	@Override
	public Set<CascadeStyle> getCascadeStyles() {
		return cascadeStyles;
	}

	@Override
	public FetchTiming getFetchTiming() {
		final String fetchSelection = manyToManyElement.getFetch() != null ?
				manyToManyElement.getFetch().value() :
				null;
		final String lazySelection = manyToManyElement.getLazy() != null
				? manyToManyElement.getLazy().value()
				: null;
		final String outerJoinSelection = manyToManyElement.getOuterJoin() != null
				? manyToManyElement.getOuterJoin().value()
				: null;

		if ( lazySelection == null ) {
			if ( "join".equals( fetchSelection ) || "true".equals( outerJoinSelection ) ) {
				return FetchTiming.IMMEDIATE;
			}
			else if ( "false".equals( outerJoinSelection ) ) {
				return FetchTiming.DELAYED;
			}
			else {
				// default is FetchTiming.IMMEDIATE.
				return FetchTiming.IMMEDIATE;
			}
		}
		else if ( "true".equals( lazySelection ) ) {
			return FetchTiming.DELAYED;
		}
		else if ( "false".equals( lazySelection ) ) {
			return FetchTiming.IMMEDIATE;
		}
		// TODO: improve this method to say attribute name.
		throw new MappingException(
				String.format(
						"Unexpected lazy selection [%s] on many-to-many element",
						lazySelection
				),
				origin()
		);
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
		return manyToManyElement.getOrderBy();
	}

	public class JoinColumnResolutionDelegateImpl implements JoinColumnResolutionDelegate {
		@Override
		public String getReferencedAttributeName() {
			return manyToManyElement.getPropertyRef();
		}

		@Override
		public List<? extends Value> getJoinColumns(JoinColumnResolutionContext context) {
			return context.resolveRelationalValuesForAttribute( manyToManyElement.getPropertyRef() );
		}

		@Override
		public TableSpecification getReferencedTable(JoinColumnResolutionContext context) {
			return context.resolveTableForAttribute( manyToManyElement.getPropertyRef() );
		}
	}

}
