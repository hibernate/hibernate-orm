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
package org.hibernate.metamodel.source.hbm.state.binding;

import java.util.Map;
import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.binding.MappingDefaults;
import org.hibernate.metamodel.domain.MetaAttribute;
import org.hibernate.metamodel.source.util.MappingHelper;
import org.hibernate.metamodel.state.binding.AttributeBindingState;

/**
 * @author Gail Badner
 */
public abstract class AbstractHbmAttributeBindingState implements AttributeBindingState {
	private final String ownerClassName;
	private final String attributeName;
	private final MappingDefaults defaults;
	private final String nodeName;
	private final String accessorName;
	private final boolean isOptimisticLockable;
	private final Map<String, MetaAttribute> metaAttributes;

	public AbstractHbmAttributeBindingState(
			String ownerClassName,
			String attributeName,
			MappingDefaults defaults,
			String nodeName,
			Map<String, MetaAttribute> metaAttributes,
			String accessorName,
			boolean isOptimisticLockable) {
		if ( attributeName == null ) {
			throw new MappingException(
					"Attribute name cannot be null."
			);
		}

		this.ownerClassName = ownerClassName;
		this.attributeName = attributeName;
		this.defaults = defaults;
		this.nodeName =  nodeName;
		this.metaAttributes = metaAttributes;
		this.accessorName = accessorName;
		this.isOptimisticLockable = isOptimisticLockable;
	}

	// TODO: really don't like this here...
	protected String getOwnerClassName() {
		return ownerClassName;
	}

	protected final String getTypeNameByReflection() {
		Class ownerClass = MappingHelper.classForName( ownerClassName, defaults.getServiceRegistry() );
		return ReflectHelper.reflectedPropertyClass( ownerClass, attributeName ).getName();
	}

	public String getAttributeName() {
		return attributeName;
	}

	protected final MappingDefaults getDefaults() {
		return defaults;
	}

	public final String getPropertyAccessorName() {
		return accessorName;
	}

	public final boolean isAlternateUniqueKey() {
		//TODO: implement
		return false;
	}

	public final boolean isOptimisticLockable() {
		return isOptimisticLockable;
	}

	public final String getNodeName() {
		return nodeName == null ? getAttributeName() : nodeName;
	}

	public final Map<String, MetaAttribute> getMetaAttributes() {
		return metaAttributes;
	}

	public PropertyGeneration getPropertyGeneration() {
		return PropertyGeneration.NEVER;
	}

	public boolean isKeyCascadeDeleteEnabled() {
		return false;
	}

	public String getUnsavedValue() {
		//TODO: implement
		return null;
	}

	public Properties getTypeParameters() {
		return null;
	}
}
