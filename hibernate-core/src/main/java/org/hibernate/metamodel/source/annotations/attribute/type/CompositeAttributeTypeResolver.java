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

package org.hibernate.metamodel.source.annotations.attribute.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;

/**
 * @author Strong Liu
 */
public class CompositeAttributeTypeResolver implements AttributeTypeResolver {
	private List<AttributeTypeResolver> resolvers = new ArrayList<AttributeTypeResolver>();
	private final AttributeTypeResolverImpl explicitHibernateTypeResolver;

	public CompositeAttributeTypeResolver(AttributeTypeResolverImpl explicitHibernateTypeResolver) {
		if ( explicitHibernateTypeResolver == null ) {
			throw new AssertionFailure( "The Given AttributeTypeResolver is null." );
		}
		this.explicitHibernateTypeResolver = explicitHibernateTypeResolver;
	}

	public void addHibernateTypeResolver(AttributeTypeResolver resolver) {
		if ( resolver == null ) {
			throw new AssertionFailure( "The Given AttributeTypeResolver is null." );
		}
		resolvers.add( resolver );
	}

	@Override
	public String getExplicitHibernateTypeName() {
		String type = explicitHibernateTypeResolver.getExplicitHibernateTypeName();
		if ( StringHelper.isEmpty( type ) ) {
			for ( AttributeTypeResolver resolver : resolvers ) {
				type = resolver.getExplicitHibernateTypeName();
				if ( StringHelper.isNotEmpty( type ) ) {
					break;
				}
			}
		}
		return type;
	}

	@Override
	public Map<String, String> getExplicitHibernateTypeParameters() {
		Map<String, String> parameters = explicitHibernateTypeResolver.getExplicitHibernateTypeParameters();
		if ( CollectionHelper.isEmpty( parameters ) ) {
			for ( AttributeTypeResolver resolver : resolvers ) {
				parameters = resolver.getExplicitHibernateTypeParameters();
				if ( CollectionHelper.isNotEmpty( parameters ) ) {
					break;
				}
			}
		}
		return parameters;
	}
}
