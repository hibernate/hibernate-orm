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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;
import org.hibernate.metamodel.spi.binding.TypeDefinition;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

/**
 * @author Strong Liu
 * @author Brett Meyer
 * @author Gail Badner
 */
public abstract class AbstractAttributeTypeResolver implements AttributeTypeResolver {
	private final String name;
	private final JavaTypeDescriptor javaType;
	private final AnnotationInstance annotation;
	private final EntityBindingContext context;

	public AbstractAttributeTypeResolver(
			String name,
			JavaTypeDescriptor javaType,
			AnnotationInstance annotation,
			EntityBindingContext context){
		this.name = name;
		this.javaType = javaType;
		this.annotation = annotation;
		this.context = context;
	}

	@Override
	final public String getExplicitHibernateTypeName() {
		String type = getExplicitAnnotatedHibernateTypeName();
		// If the attribute is annotated with a type, use it.  Otherwise,
		// check for a @TypeDef.
		if ( !StringHelper.isEmpty( type ) ) {
			return type;
		}
		else {
			return hasTypeDef() ? javaType().getName().toString() : null;
		}
	}
	
	@Override
	final public String getExplicitAnnotatedHibernateTypeName() {
		return resolveHibernateTypeName();
	}

	@Override
	final public Map<String, String> getExplicitHibernateTypeParameters() {
		Map<String, String> result = new HashMap<String, String>(  );
		//this is only use by enum type and serializable blob type, but we put there anyway
		// TODO: why?
		//result.put(
		//		DynamicParameterizedType.RETURNED_CLASS,
		//		javaClass().getName()
		//);
		if ( StringHelper.isNotEmpty( getExplicitHibernateTypeName() ) ) {
			result.putAll( resolveHibernateTypeParameters() );
		}
		return result;
	}

	protected final TypeDefinition getTypeDefinition() {
		return context.getMetadataCollector().getTypeDefinition(
				javaType().getName().toString()
		);
	}

	protected final boolean hasTypeDef() {
		return context.getMetadataCollector().hasTypeDefinition(
				javaType().getName().toString()
		);
	}

	protected abstract String resolveHibernateTypeName();

	protected Map<String, String> resolveHibernateTypeParameters() {
		return Collections.emptyMap();
	}

	protected String name() {
		return name;
	}

	protected JavaTypeDescriptor javaType() {
		return javaType;
	}

	protected AnnotationInstance annotation() {
		return annotation;
	}

	protected static AnnotationInstance resolveAnnotationInstance(
			Map<DotName, List<AnnotationInstance>> annotations,
			DotName dotName) {
		return JandexHelper.getSingleAnnotation(
				annotations,
				dotName
		);
	}
	
	protected EntityBindingContext getContext() {
		return context;
	}
}
