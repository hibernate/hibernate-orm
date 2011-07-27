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
package org.hibernate.metamodel.source.annotations.entity;

import java.util.List;

import org.hibernate.internal.util.Value;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.source.LocalBindingContext;
import org.hibernate.metamodel.source.binder.AttributeSource;
import org.hibernate.metamodel.source.binder.ComponentAttributeSource;
import org.hibernate.metamodel.source.binder.ExplicitHibernateTypeSource;
import org.hibernate.metamodel.source.binder.MetaAttributeSource;
import org.hibernate.metamodel.source.binder.RelationalValueSource;
import org.hibernate.metamodel.source.binder.SingularAttributeNature;

/**
 * @author Steve Ebersole
 */
public class ComponentAttributeSourceImpl implements ComponentAttributeSource {
	private final EmbeddableClass component;
	private final ConfiguredClass parent;

	private final Value<Class<?>> classReference;

	public ComponentAttributeSourceImpl(EmbeddableClass component, ConfiguredClass parent) {
		this.component = component;
		this.parent = parent;

		this.classReference = new Value<Class<?>>( component.getClass() );
	}

	@Override
	public String getClassName() {
		return component.getClass().getName();
	}

	@Override
	public Value<Class<?>> getClassReference() {
		return classReference;
	}

	@Override
	public String getParentReferenceAttributeName() {
		// todo : do annotations support this?
		return null;
	}

	@Override
	public String getPath() {
		// todo : implement
		// do not see how this is possible currently given how annotations currently handle components
		return null;
	}

	@Override
	public Iterable<AttributeSource> attributeSources() {
		// todo : implement
		return null;
	}

	@Override
	public LocalBindingContext getLocalBindingContext() {
		return component.getLocalBindingContext();
	}

	@Override
	public boolean isVirtualAttribute() {
		// todo : verify
		return false;
	}

	@Override
	public SingularAttributeNature getNature() {
		return SingularAttributeNature.COMPONENT;
	}

	@Override
	public ExplicitHibernateTypeSource getTypeInformation() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String getName() {
		// todo : implement
		// do not see how this is possible currently given how annotations currently handle components
		return null;
	}

	@Override
	public boolean isSingular() {
		return true;
	}

	@Override
	public String getPropertyAccessorName() {
		// todo : implement
		return null;
	}

	@Override
	public boolean isInsertable() {
		// todo : implement
		return true;
	}

	@Override
	public boolean isUpdatable() {
		// todo : implement
		return true;
	}

	@Override
	public PropertyGeneration getGeneration() {
		// todo : implement
		return null;
	}

	@Override
	public boolean isLazy() {
		// todo : implement
		return false;
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		// todo : implement
		return true;
	}

	@Override
	public Iterable<MetaAttributeSource> metaAttributes() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean areValuesIncludedInInsertByDefault() {
		// todo : implement
		return true;
	}

	@Override
	public boolean areValuesIncludedInUpdateByDefault() {
		// todo : implement
		return true;
	}

	@Override
	public boolean areValuesNullableByDefault() {
		// todo : implement
		return true;
	}

	@Override
	public List<RelationalValueSource> relationalValueSources() {
		return null;
	}
}
