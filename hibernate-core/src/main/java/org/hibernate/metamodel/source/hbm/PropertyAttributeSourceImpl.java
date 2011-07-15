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
package org.hibernate.metamodel.source.hbm;

import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.source.LocalBindingContext;
import org.hibernate.metamodel.source.binder.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.source.binder.MetaAttributeSource;
import org.hibernate.metamodel.source.binder.RelationalValueSource;
import org.hibernate.metamodel.source.binder.SingularAttributeNature;
import org.hibernate.metamodel.source.binder.SingularAttributeSource;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLPropertyElement;

/**
 * Implementation for {@code <property/>} mappings
 *
 * @author Steve Ebersole
 */
class PropertyAttributeSourceImpl implements SingularAttributeSource {
	private final XMLPropertyElement propertyElement;
	private final ExplicitHibernateTypeSource typeSource;
	private final List<RelationalValueSource> valueSources;

	PropertyAttributeSourceImpl(final XMLPropertyElement propertyElement, LocalBindingContext bindingContext) {
		this.propertyElement = propertyElement;
		this.typeSource = new ExplicitHibernateTypeSource() {
			private final String name = propertyElement.getTypeAttribute() != null
					? propertyElement.getTypeAttribute()
					: propertyElement.getType() != null
							? propertyElement.getType().getName()
							: null;
			private final Map<String, String> parameters = ( propertyElement.getType() != null )
					? Helper.extractParameters( propertyElement.getType().getParam() )
					: null;

			@Override
			public String getName() {
				return name;
			}

			@Override
			public Map<String, String> getParameters() {
				return parameters;
			}
		};
		this.valueSources = Helper.buildValueSources(
				new Helper.ValueSourcesAdapter() {
					@Override
					public String getColumnAttribute() {
						return propertyElement.getColumn();
					}

					@Override
					public String getFormulaAttribute() {
						return propertyElement.getFormula();
					}

					@Override
					public List getColumnOrFormulaElements() {
						return propertyElement.getColumnOrFormula();
					}
				},
				bindingContext
		);
	}

	@Override
	public String getName() {
		return propertyElement.getName();
	}

	@Override
	public ExplicitHibernateTypeSource getTypeInformation() {
		return typeSource;
	}

	@Override
	public String getPropertyAccessorName() {
		return propertyElement.getAccess();
	}

	@Override
	public boolean isInsertable() {
		return Helper.getBooleanValue( propertyElement.isInsert(), true );
	}

	@Override
	public boolean isUpdatable() {
		return Helper.getBooleanValue( propertyElement.isUpdate(), true );
	}

	@Override
	public PropertyGeneration getGeneration() {
		return PropertyGeneration.parse( propertyElement.getGenerated() );
	}

	@Override
	public boolean isLazy() {
		return Helper.getBooleanValue( propertyElement.isLazy(), false );
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return Helper.getBooleanValue( propertyElement.isOptimisticLock(), true );
	}

	@Override
	public Iterable<CascadeStyle> getCascadeStyle() {
		return Helper.NO_CASCADING;
	}

	@Override
	public SingularAttributeNature getNature() {
		return SingularAttributeNature.BASIC;
	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return valueSources;
	}

	@Override
	public boolean isSingular() {
		return true;
	}

	@Override
	public Iterable<MetaAttributeSource> metaAttributes() {
		return Helper.buildMetaAttributeSources( propertyElement.getMeta() );
	}
}
