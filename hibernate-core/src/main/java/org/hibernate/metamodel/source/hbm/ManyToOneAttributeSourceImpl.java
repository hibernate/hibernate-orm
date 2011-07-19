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

import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.source.LocalBindingContext;
import org.hibernate.metamodel.source.binder.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.source.binder.MetaAttributeSource;
import org.hibernate.metamodel.source.binder.RelationalValueSource;
import org.hibernate.metamodel.source.binder.SingularAttributeNature;
import org.hibernate.metamodel.source.binder.ToOneAttributeSource;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLManyToOneElement;

/**
 * Implementation for {@code <many-to-one/> mappings}
 *
 * @author Steve Ebersole
 */
class ManyToOneAttributeSourceImpl implements ToOneAttributeSource {
	private final XMLManyToOneElement manyToOneElement;
	private final LocalBindingContext bindingContext;
	private final List<RelationalValueSource> valueSources;

	ManyToOneAttributeSourceImpl(final XMLManyToOneElement manyToOneElement, LocalBindingContext bindingContext) {
		this.manyToOneElement = manyToOneElement;
		this.bindingContext = bindingContext;
		this.valueSources = Helper.buildValueSources(
				new Helper.ValueSourcesAdapter() {
					@Override
					public String getColumnAttribute() {
						return manyToOneElement.getColumn();
					}

					@Override
					public String getFormulaAttribute() {
						return manyToOneElement.getFormula();
					}

					@Override
					public List getColumnOrFormulaElements() {
						return manyToOneElement.getColumnOrFormula();
					}
				},
				bindingContext
		);
	}

	@Override
	public String getName() {
			return manyToOneElement.getName();
	}

	@Override
	public ExplicitHibernateTypeSource getTypeInformation() {
		return Helper.TO_ONE_ATTRIBUTE_TYPE_SOURCE;
	}

	@Override
	public String getPropertyAccessorName() {
		return manyToOneElement.getAccess();
	}

	@Override
	public boolean isInsertable() {
		return manyToOneElement.isInsert();
	}

	@Override
	public boolean isUpdatable() {
		return manyToOneElement.isUpdate();
	}

	@Override
	public PropertyGeneration getGeneration() {
		return PropertyGeneration.NEVER;
	}

	@Override
	public boolean isLazy() {
		return false;
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return manyToOneElement.isOptimisticLock();
	}

	@Override
	public Iterable<CascadeStyle> getCascadeStyles() {
		return Helper.interpretCascadeStyles( manyToOneElement.getCascade(), bindingContext );
	}

	@Override
	public SingularAttributeNature getNature() {
		return SingularAttributeNature.MANY_TO_ONE;
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
		return Helper.buildMetaAttributeSources( manyToOneElement.getMeta() );
	}

	@Override
	public String getReferencedEntityName() {
		return manyToOneElement.getClazz() != null
				? manyToOneElement.getClazz()
				: manyToOneElement.getEntityName();
	}

	@Override
	public String getReferencedEntityAttributeName() {
		return manyToOneElement.getPropertyRef();
	}
}
