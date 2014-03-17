/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.annotations.AnnotationBindingContext;

/**
 * An AttributeTypeResolver that allows aggregation of other
 * AttributeTypeResolvers (composition)
 *
 * @author Steve Ebersole
 * @author Strong Liu
 * @author Brett Meyer
 */
public class AttributeTypeResolverComposition implements AttributeTypeResolver {
	private final String inferredJavaTypeName;
	private final AnnotationBindingContext context;
	private final List<AttributeTypeResolver> resolvers;

	public AttributeTypeResolverComposition(
			JavaTypeDescriptor inferredJavaType,
			AnnotationBindingContext context) {
		this( inferredJavaType, context, Collections.<AttributeTypeResolver>emptyList() );
	}

	public AttributeTypeResolverComposition(
			JavaTypeDescriptor inferredJavaType,
			AnnotationBindingContext context,
			AttributeTypeResolver... resolvers) {
		this( inferredJavaType, context, Arrays.asList( resolvers ) );
	}

	public AttributeTypeResolverComposition(
			JavaTypeDescriptor inferredJavaType,
			AnnotationBindingContext context,
			List<AttributeTypeResolver> resolvers) {
		this.inferredJavaTypeName = inferredJavaType.getName().toString();
		this.context = context;
		this.resolvers = resolvers;
	}

	@Override
	public String getExplicitHibernateTypeName() {
		final String resolvedType = getExplicitAnnotatedHibernateTypeName();
		if ( resolvedType != null ) {
			return resolvedType;
		}

		if ( context.getMetadataCollector().hasTypeDefinition( inferredJavaTypeName ) ) {
			return inferredJavaTypeName;
		}

		return null;
	}

	@Override
	public String getExplicitAnnotatedHibernateTypeName() {
		for ( AttributeTypeResolver resolver : resolvers ) {
			final String result = resolver.getExplicitAnnotatedHibernateTypeName();
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	@Override
	public Map<String, String> getExplicitHibernateTypeParameters() {
		for ( AttributeTypeResolver resolver : resolvers ) {
			final Map<String, String> result = resolver.getExplicitHibernateTypeParameters();
			if ( result != null && !result.isEmpty() ) {
				return result;
			}
		}
		return null;
	}
}
