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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.source.BindingContext;
import org.hibernate.metamodel.source.MappingDefaults;
import org.hibernate.metamodel.source.MetaAttributeContext;
import org.hibernate.metamodel.source.hbm.Helper;
import org.hibernate.metamodel.binding.CascadeType;
import org.hibernate.metamodel.binding.state.AttributeBindingState;

/**
 * @author Gail Badner
 */
public abstract class AbstractHbmAttributeBindingState implements AttributeBindingState {
	private final String ownerClassName;
	private final String attributeName;
	private final BindingContext bindingContext;
	private final String nodeName;
	private final String accessorName;
	private final boolean isOptimisticLockable;
	private final MetaAttributeContext metaAttributeContext;

	public AbstractHbmAttributeBindingState(
			String ownerClassName,
			String attributeName,
			BindingContext bindingContext,
			String nodeName,
			MetaAttributeContext metaAttributeContext,
			String accessorName,
			boolean isOptimisticLockable) {
		if ( attributeName == null ) {
			throw new MappingException(
					"Attribute name cannot be null."
			);
		}

		this.ownerClassName = ownerClassName;
		this.attributeName = attributeName;
		this.bindingContext = bindingContext;
		this.nodeName = nodeName;
		this.metaAttributeContext = metaAttributeContext;
		this.accessorName = accessorName;
		this.isOptimisticLockable = isOptimisticLockable;
	}

	// TODO: really don't like this here...
	protected String getOwnerClassName() {
		return ownerClassName;
	}

	protected Set<CascadeType> determineCascadeTypes(String cascade) {
		String commaSeparatedCascades = Helper.getStringValue(
				cascade,
				getBindingContext().getMappingDefaults()
						.getCascadeStyle()
		);
		Set<String> cascades = Helper.getStringValueTokens( commaSeparatedCascades, "," );
		Set<CascadeType> cascadeTypes = new HashSet<CascadeType>( cascades.size() );
		for ( String s : cascades ) {
			CascadeType cascadeType = CascadeType.getCascadeType( s );
			if ( cascadeType == null ) {
				throw new MappingException( "Invalid cascading option " + s );
			}
			cascadeTypes.add( cascadeType );
		}
		return cascadeTypes;
	}

	protected final String getTypeNameByReflection() {
		Class ownerClass = bindingContext.locateClassByName( ownerClassName );
		return ReflectHelper.reflectedPropertyClass( ownerClass, attributeName ).getName();
	}

	public String getAttributeName() {
		return attributeName;
	}

	public BindingContext getBindingContext() {
		return bindingContext;
	}

	@Deprecated
	protected final MappingDefaults getDefaults() {
		return getBindingContext().getMappingDefaults();
	}

	@Override
	public final String getPropertyAccessorName() {
		return accessorName;
	}

	@Override
	public final boolean isAlternateUniqueKey() {
		//TODO: implement
		return false;
	}

	@Override
	public final boolean isOptimisticLockable() {
		return isOptimisticLockable;
	}

	@Override
	public final String getNodeName() {
		return nodeName == null ? getAttributeName() : nodeName;
	}

	@Override
	public MetaAttributeContext getMetaAttributeContext() {
		return metaAttributeContext;
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

	public Map<String, String> getExplicitHibernateTypeParameters() {
		return null;
	}
}
