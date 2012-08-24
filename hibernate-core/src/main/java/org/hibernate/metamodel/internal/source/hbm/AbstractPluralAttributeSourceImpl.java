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

import org.hibernate.FetchMode;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jaxb.spi.hbm.PluralAttributeElement;
import org.hibernate.metamodel.spi.binding.Caching;
import org.hibernate.metamodel.spi.binding.CustomSQL;
import org.hibernate.metamodel.spi.source.AttributeSourceContainer;
import org.hibernate.metamodel.spi.source.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.PluralAttributeElementSource;
import org.hibernate.metamodel.spi.source.PluralAttributeKeySource;
import org.hibernate.metamodel.spi.source.PluralAttributeSource;
import org.hibernate.metamodel.spi.source.TableSpecificationSource;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractPluralAttributeSourceImpl
		extends AbstractHbmSourceNode
		implements PluralAttributeSource, Helper.InLineViewNameInferrer {
	private final PluralAttributeElement pluralAttributeElement;
	private final AttributeSourceContainer container;

	private final ExplicitHibernateTypeSource typeInformation;

	private final PluralAttributeKeySource keySource;
	private final PluralAttributeElementSource elementSource;
	private final Caching caching;

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

		this.typeInformation = new ExplicitHibernateTypeSource() {
			@Override
			public String getName() {
				return pluralAttributeElement.getCollectionType();
			}

			@Override
			public Map<String, String> getParameters() {
				return Collections.emptyMap();
			}
		};
	}

	private PluralAttributeElementSource interpretElementType() {
		if ( pluralAttributeElement.getElement() != null ) {
			return new BasicPluralAttributeElementSourceImpl(
					sourceMappingDocument(),
					pluralAttributeElement.getElement()
			);
		}
		else if ( pluralAttributeElement.getCompositeElement() != null ) {
			return new CompositePluralAttributeElementSourceImpl(
					sourceMappingDocument(),
					pluralAttributeElement.getCompositeElement()
			);
		}
		else if ( pluralAttributeElement.getOneToMany() != null ) {
			return new OneToManyPluralAttributeElementSourceImpl(
					sourceMappingDocument(),
					pluralAttributeElement,
					pluralAttributeElement.getOneToMany()
			);
		}
		else if ( pluralAttributeElement.getManyToMany() != null ) {
			return new ManyToManyPluralAttributeElementSourceImpl(
					sourceMappingDocument(),
					pluralAttributeElement,
					pluralAttributeElement.getManyToMany()
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

	public PluralAttributeElement getPluralAttributeElement() {
		return pluralAttributeElement;
	}

	protected AttributeSourceContainer container() {
		return container;
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
	public ExplicitHibernateTypeSource getTypeInformation() {
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
			if ( "join".equals( fetchSelection ) || "true".equals( outerJoinSelection ) ) {
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
			return FetchTiming.EXTRA_DELAYED;
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
		final int batchSize = pluralAttributeElement.getBatchSize();

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

	@Override
	public FetchMode getFetchMode() {
		return pluralAttributeElement.getFetch() == null
				? null
				: FetchMode.valueOf( pluralAttributeElement.getFetch().value() );
	}
}
