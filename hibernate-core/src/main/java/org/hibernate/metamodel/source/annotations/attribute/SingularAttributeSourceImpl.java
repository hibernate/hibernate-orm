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
package org.hibernate.metamodel.source.annotations.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.source.binder.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.source.binder.MetaAttributeSource;
import org.hibernate.metamodel.source.binder.RelationalValueSource;
import org.hibernate.metamodel.source.binder.SingularAttributeNature;
import org.hibernate.metamodel.source.binder.SingularAttributeSource;

/**
 * @author Hardy Ferentschik
 */
public class SingularAttributeSourceImpl implements SingularAttributeSource {
	private final MappedAttribute attribute;
	private final AttributeOverride attributeOverride;

	public SingularAttributeSourceImpl(MappedAttribute attribute) {
		this(attribute, null);
	}

	public SingularAttributeSourceImpl(MappedAttribute attribute, AttributeOverride attributeOverride) {
		this.attribute = attribute;
		this.attributeOverride = attributeOverride;
	}

	@Override
	public ExplicitHibernateTypeSource getTypeInformation() {
		return new ExplicitHibernateTypeSourceImpl( attribute.getHibernateTypeResolver() );
	}

	@Override
	public String getPropertyAccessorName() {
		return attribute.getAccessType();
	}

	@Override
	public boolean isInsertable() {
		return attribute.isInsertable();
	}

	@Override
	public boolean isUpdatable() {
		return attribute.isUpdatable();
	}

	@Override
	public PropertyGeneration getGeneration() {
		return attribute.getPropertyGeneration();
	}

	@Override
	public boolean isLazy() {
		return attribute.isLazy();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return attribute.isOptimisticLockable();
	}

	@Override
	public String getName() {
		return attribute.getName();
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		List<RelationalValueSource> valueSources = new ArrayList<RelationalValueSource>();
		valueSources.add( new ColumnSourceImpl( attribute, attributeOverride ) );
		return valueSources;
	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	public boolean isSingular() {
		return true;
	}

	@Override
	public SingularAttributeNature getNature() {
		return SingularAttributeNature.BASIC;
	}

	@Override
	public Iterable<MetaAttributeSource> metaAttributes() {
		return Collections.emptySet();
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
		return true;
	}
}


