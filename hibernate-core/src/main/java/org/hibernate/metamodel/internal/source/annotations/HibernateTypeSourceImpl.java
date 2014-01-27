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
package org.hibernate.metamodel.internal.source.annotations;

import java.util.Map;

import org.hibernate.internal.util.ValueHolder;
import org.hibernate.metamodel.internal.source.annotations.attribute.MappedAttribute;
import org.hibernate.metamodel.spi.source.HibernateTypeSource;

/**
 * @author Hardy Ferentschik
 * @author Strong Liu
 */
public class HibernateTypeSourceImpl implements HibernateTypeSource {
	private final ValueHolder<String> nameHolder;
	private final ValueHolder<Map<String, String>> parameterHolder;
	private final Class javaType;

	public HibernateTypeSourceImpl(final MappedAttribute attribute) {
		this.nameHolder = new ValueHolder<String>(
				new ValueHolder.DeferredInitializer<String>() {
					@Override
					public String initialize() {
						return attribute.getHibernateTypeResolver().getExplicitHibernateTypeName();
					}
				}
		);
		this.parameterHolder = new ValueHolder<Map<String, String>>(
				new ValueHolder.DeferredInitializer<Map<String, String>>() {
					@Override
					public Map<String, String> initialize() {
						return attribute.getHibernateTypeResolver().getExplicitHibernateTypeParameters();
					}
				}
		);
		this.javaType = attribute.getAttributeType();
	}

	@Override
	public String getName() {
		return nameHolder.getValue();
	}

	@Override
	public Map<String, String> getParameters() {
		return parameterHolder.getValue();
	}

	@Override
	public Class getJavaType() {
		return javaType;
	}
}


