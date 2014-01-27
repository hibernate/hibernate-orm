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

import java.util.Collections;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.ValueHolder;
import org.hibernate.jaxb.spi.hbm.JaxbFilterElement;
import org.hibernate.jaxb.spi.hbm.PluralAttributeElement;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.source.AttributeSourceContainer;
import org.hibernate.metamodel.spi.source.AttributeSourceResolutionContext;
import org.hibernate.metamodel.spi.source.HibernateTypeSource;
import org.hibernate.metamodel.spi.source.FilterSource;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.PluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.PluralAttributeKeySource;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;
import org.hibernate.metamodel.spi.source.TableSpecificationSource;

/**
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public abstract class AbstractPluralAttributeSourceImpl
		extends AbstractHbmSourceNode
		implements PluralAttributeSource, Helper.InLineViewNameInferrer {
	private final PluralAttributeElement pluralAttributeElement;
	private final AttributeSourceContainer container;

	private final HibernateTypeSource typeInformation;

	private final PluralAttributeKeySource keySource;
	private final PluralAttributeElementSource elementSource;
	private final Caching caching;
	private final FilterSource[] filterSources;
	private ValueHolder<Class<?>> elementClassReference;

	protected AbstractPluralAttributeSourceImpl(
			MappingDocument sourceMappingDocument,
			final PluralAttributeElement pluralAttributeElement,
			AttributeSourceContainer container) {
		super( sourceMappingDocument );
		this.pluralAttributeElement = pluralAttributeElement;
		this.container = container;

		this.keySource = new PluralAttributeKeySourceImpl(
				sourceMappingDocument(),
				pluralAttributeElement.getKey(),
				container
		);
		this.elementSource = interpretElementType();

		this.caching = Helper.createCaching(
				pluralAttributeElement.getCache(),
				StringHelper.qualify( container().getPath(), getName() )
		);

		this.typeInformation = new HibernateTypeSource() {
			@Override
			public String getName() {
				return pluralAttributeElement.getCollectionType();
			}

			@Override
			public Map<String, String> getParameters() {
				return Collections.emptyMap();
			}
			@Override
			public Class getJavaType() {
				return null;
			}
		};
		this.filterSources = buildFilterSources();
	}

	private FilterSource[] buildFilterSources() {
		final int size = pluralAttributeElement.getFilter().size();
		if ( size == 0 ) {
			return null;
		}

		FilterSource[] results = new FilterSource[size];
		for ( int i = 0; i < size; i++ ) {
			JaxbFilterElement element = pluralAttributeElement.getFilter().get( i );
			results[i] = new FilterSourceImpl( sourceMappingDocument(), element );
		}
		return results;

	}

	private PluralAttributeElementSource interpretElementType() {
		if ( pluralAttributeElement.getElement() != null ) {
			// TODO: Is elementClassReference even needed in this context?
			// If so, getType is currently null.
//			elementClassReference = makeClassReference(pluralAttributeElement
//					.getElement().getType().getName());
			return new BasicPluralAttributeElementSourceImpl(
					sourceMappingDocument(),
					pluralAttributeElement.getElement()
			);
		}
		else if ( pluralAttributeElement.getCompositeElement() != null ) {
			elementClassReference = makeClassReference(pluralAttributeElement
					.getCompositeElement().getClazz());
			return new CompositePluralAttributeElementSourceImpl(
					sourceMappingDocument(),
					pluralAttributeElement.getCompositeElement(),
					pluralAttributeElement.getCascade()
			);
		}
		else if ( pluralAttributeElement.getOneToMany() != null ) {
			elementClassReference = makeClassReference(pluralAttributeElement
					.getOneToMany().getClazz());
			return new OneToManyPluralAttributeElementSourceImpl(
					sourceMappingDocument(),
					this,
					pluralAttributeElement.getOneToMany(),
					pluralAttributeElement.getCascade()
			);
		}
		else if ( pluralAttributeElement.getManyToMany() != null ) {
			elementClassReference = makeClassReference(pluralAttributeElement
					.getManyToMany().getClazz());
			return new ManyToManyPluralAttributeElementSourceImpl(
					sourceMappingDocument(),
					this,
					pluralAttributeElement.getManyToMany(),
					pluralAttributeElement.getCascade()
			);
		}
		else if ( pluralAttributeElement.getManyToAny() != null ) {
			throw new NotYetImplementedException( "Support for many-to-any not yet implemented" );
//			return Nature.MANY_TO_ANY;
		}
		else {
			throw new MappingException(
					"Unexpected collection element type : " + pluralAttributeElement.getName(),
					bindingContext().getOrigin()
			);
		}
	}

	@Override
	public PluralAttributeElementSource resolvePluralAttributeElementSource(AttributeSourceResolutionContext context) {
		// elementSource is already resolved; nothing to do.
		return elementSource;
	}

	@Override
	public boolean usesJoinTable() {
		switch ( elementSource.getNature() ) {
			case BASIC:
			case AGGREGATE:
			case ONE_TO_MANY:
				return false;
			case MANY_TO_MANY:
				return true;
			case MANY_TO_ANY:
				throw new NotYetImplementedException(
						String.format( "%s is not implemented yet.", elementSource.getNature() )
				);
			default:
				throw new AssertionFailure(
						String.format(
								"Unexpected plural attribute element source nature: %s",
								elementSource.getNature()
						)
				);
		}
	}

	public PluralAttributeElement getPluralAttributeElement() {
		return pluralAttributeElement;
	}

	protected AttributeSourceContainer container() {
		return container;
	}

	@Override
	public FilterSource[] getFilterSources() {
		return filterSources;
	}

	@Override
	public PluralAttributeKeySource getKeySource() {
		return keySource;
	}

	@Override
	public PluralAttributeElementSource getElementSource() {
		return elementSource;
	}

	@Override
	public ValueHolder<Class<?>> getElementClassReference() {
		return elementClassReference;
	}

	@Override
	public boolean isMutable() {
		return pluralAttributeElement.isMutable();
	}

	@Override
	public int getBatchSize() {
		return pluralAttributeElement.getBatchSize();
	}

	@Override
	public String getMappedBy() {
		return null;
	}

	@Override
	public String inferInLineViewName() {
		return container().getPath() + "." + pluralAttributeElement.getName();
	}

	@Override
	public TableSpecificationSource getCollectionTableSpecificationSource() {
		return pluralAttributeElement.getOneToMany() == null ?
				Helper.createTableSource( sourceMappingDocument(), pluralAttributeElement, this ) :
				null;
	}

	@Override
	public String getCollectionTableComment() {
		return pluralAttributeElement.getComment();
	}

	@Override
	public String getCollectionTableCheck() {
		return pluralAttributeElement.getCheck();
	}

	@Override
	public Caching getCaching() {
		return caching;
	}

	@Override
	public String getWhere() {
		return pluralAttributeElement.getWhere();
	}

	@Override
	public String getName() {
		return pluralAttributeElement.getName();
	}

	@Override
	public boolean isSingular() {
		return false;
	}

	@Override
	public HibernateTypeSource getTypeInformation() {
		return typeInformation;
	}

	@Override
	public String getPropertyAccessorName() {
		return pluralAttributeElement.getAccess();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return pluralAttributeElement.isOptimisticLock();
	}

	@Override
	public boolean isInverse() {
		return pluralAttributeElement.isInverse();
	}

	@Override
	public String getCustomPersisterClassName() {
		return pluralAttributeElement.getPersister();
	}

	@Override
	public String getCustomLoaderName() {
		return pluralAttributeElement.getLoader() == null
				? null
				: pluralAttributeElement.getLoader().getQueryRef();
	}

	@Override
	public CustomSQL getCustomSqlInsert() {
		return Helper.buildCustomSql( pluralAttributeElement.getSqlInsert() );
	}

	@Override
	public CustomSQL getCustomSqlUpdate() {
		return Helper.buildCustomSql( pluralAttributeElement.getSqlUpdate() );
	}

	@Override
	public CustomSQL getCustomSqlDelete() {
		return Helper.buildCustomSql( pluralAttributeElement.getSqlDelete() );
	}

	@Override
	public CustomSQL getCustomSqlDeleteAll() {
		return Helper.buildCustomSql( pluralAttributeElement.getSqlDeleteAll() );
	}

	@Override
	public Iterable<? extends MetaAttributeSource> getMetaAttributeSources() {
		return pluralAttributeElement.getMeta();
	}

	@Override
	public FetchTiming getFetchTiming() {
		final String fetchSelection = pluralAttributeElement.getFetch() != null ?
				pluralAttributeElement.getFetch().value() :
				null;
		final String lazySelection = pluralAttributeElement.getLazy() != null
				? pluralAttributeElement.getLazy().value()
				: null;
		final String outerJoinSelection = pluralAttributeElement.getOuterJoin() != null
				? pluralAttributeElement.getOuterJoin().value()
				: null;

		if ( lazySelection == null ) {
			if ( "join".equals( fetchSelection ) && "true".equals( outerJoinSelection ) ) {
				return FetchTiming.IMMEDIATE;
			}
			else if ( "false".equals( outerJoinSelection ) ) {
				return FetchTiming.DELAYED;
			}
			else {
				return bindingContext().getMappingDefaults().areAssociationsLazy()
						? FetchTiming.DELAYED
						: FetchTiming.IMMEDIATE;
			}
		}
		else  if ( "extra".equals( lazySelection ) ) {
			return FetchTiming.EXTRA_LAZY;
		}
		else if ( "true".equals( lazySelection ) ) {
			return FetchTiming.DELAYED;
		}
		else if ( "false".equals( lazySelection ) ) {
			return FetchTiming.IMMEDIATE;
		}

		throw new MappingException(
				String.format(
						"Unexpected lazy selection [%s] on '%s'",
						lazySelection,
						pluralAttributeElement.getName()
				),
				origin()
		);
	}

	@Override
	public FetchStyle getFetchStyle() {
		final String fetchSelection = pluralAttributeElement.getFetch() != null ?
				pluralAttributeElement.getFetch().value() :
				null;
		final String outerJoinSelection = pluralAttributeElement.getOuterJoin() != null
				? pluralAttributeElement.getOuterJoin().value()
				: null;
		final int batchSize = getBatchSize();

		if ( fetchSelection == null ) {
			if ( outerJoinSelection == null ) {
				return batchSize > 1 ? FetchStyle.BATCH : FetchStyle.SELECT;
			}
			else {
				if ( "auto".equals( outerJoinSelection ) ) {
					return bindingContext().getMappingDefaults().areAssociationsLazy()
							? FetchStyle.SELECT
							: FetchStyle.JOIN;
				}
				else {
					return "true".equals( outerJoinSelection ) ? FetchStyle.JOIN : FetchStyle.SELECT;
				}
			}
		}
		else {
			if ( "subselect".equals( fetchSelection ) ) {
				return FetchStyle.SUBSELECT;
			}
			else {
				return "join".equals( fetchSelection ) ? FetchStyle.JOIN : FetchStyle.SELECT;
			}
		}
	}
}
