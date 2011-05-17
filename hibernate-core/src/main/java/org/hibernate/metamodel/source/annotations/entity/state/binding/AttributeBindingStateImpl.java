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
package org.hibernate.metamodel.source.annotations.entity.state.binding;

import java.util.Map;
import java.util.Properties;

import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.binding.state.SimpleAttributeBindingState;
import org.hibernate.metamodel.domain.MetaAttribute;
import org.hibernate.metamodel.source.annotations.entity.MappedAttribute;

/**
 * Implementation of the attribute binding state via annotation configuration.
 *
 * @author Hardy Ferentschik
 */
public class AttributeBindingStateImpl implements SimpleAttributeBindingState {
	private final MappedAttribute mappedAttribute;
	private final PropertyGeneration propertyGeneration = null;
	private final String typeName;
	private final Properties typeParameters;

	public AttributeBindingStateImpl(MappedAttribute mappedAttribute) {
		this.mappedAttribute = mappedAttribute;
		typeName = mappedAttribute.getType().getName();
		// TODO: implement....
		typeParameters = null;
	}

	@Override
	public String getAttributeName() {
		return mappedAttribute.getName();
	}

	@Override
	public PropertyGeneration getPropertyGeneration() {

//		GeneratedValue generatedValue = property.getAnnotation( GeneratedValue.class );
//		String generatorType = generatedValue != null ?
//				generatorType( generatedValue.strategy(), mappings ) :
//				"assigned";
//		String generatorName = generatedValue != null ?
//				generatedValue.generator() :
//				BinderHelper.ANNOTATION_STRING_DEFAULT;
		return propertyGeneration;
	}

	@Override
	public boolean isInsertable() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean isUpdateable() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean isKeyCascadeDeleteEnabled() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String getUnsavedValue() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String getTypeName() {
		return typeName;
	}

	@Override
	public Properties getTypeParameters() {
		return typeParameters;
	}

	@Override
	public boolean isLazy() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String getPropertyAccessorName() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean isAlternateUniqueKey() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String getCascade() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean isOptimisticLockable() {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String getNodeName() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public Map<String, MetaAttribute> getMetaAttributes() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}


