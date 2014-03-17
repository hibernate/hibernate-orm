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

package org.hibernate.metamodel.source.internal.annotations.attribute.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.source.internal.annotations.attribute.AbstractPersistentAttribute;

/**
 * @author Strong Liu
 * @author Brett Meyer
 */
public class CompositeAttributeTypeResolver implements AttributeTypeResolver {
	private final AbstractPersistentAttribute persistentAttribute;
	private List<AttributeTypeResolver> resolvers = new ArrayList<AttributeTypeResolver>();
	private AttributeTypeResolver theResolver;

	public CompositeAttributeTypeResolver(AbstractPersistentAttribute persistentAttribute,
										  AttributeTypeResolver... resolvers) {
		this.persistentAttribute = persistentAttribute;
		this.resolvers.addAll( Arrays.asList( resolvers ) );
	}

	public void addHibernateTypeResolver(AttributeTypeResolver resolver) {
		if ( resolver == null ) {
			throw new AssertionFailure( "The Given AttributeTypeResolver is null." );
		}
		resolvers.add( resolver );
	}

	@Override
	public String getExplicitHibernateTypeName() {
		String type = getExplicitAnnotatedHibernateTypeName();
		if ( StringHelper.isNotEmpty( type ) ) {
			return type;
		}

		// todo : isn't this kind of the anti-thesis of an *explicit* name?
		final String attributeTypeName = persistentAttribute.getBackingMember().getType().getErasedType().getName().toString();
		if ( persistentAttribute.getContext().getMetadataCollector().hasTypeDefinition( attributeTypeName ) ) {
			return attributeTypeName;
		}

		return null;
	}

	@Override
	public String getExplicitAnnotatedHibernateTypeName() {
		if ( theResolver != null ) {
			return theResolver.getExplicitAnnotatedHibernateTypeName();
		}
		for ( AttributeTypeResolver resolver : resolvers ) {
			String type = resolver.getExplicitAnnotatedHibernateTypeName();
			if ( StringHelper.isNotEmpty( type ) ) {
				theResolver = resolver;
				return type;
			}
		}
		return null;
	}

	@Override
	public Map<String, String> getExplicitHibernateTypeParameters() {
		if ( theResolver != null ) {
			return theResolver.getExplicitHibernateTypeParameters();
		}
		for ( AttributeTypeResolver resolver : resolvers ) {
			Map<String, String> parameters = resolver.getExplicitHibernateTypeParameters();
			if ( CollectionHelper.isNotEmpty( parameters ) ) {
				return parameters;
			}
		}
		return Collections.EMPTY_MAP;
	}
}
