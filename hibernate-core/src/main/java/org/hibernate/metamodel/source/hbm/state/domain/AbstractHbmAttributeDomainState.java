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
package org.hibernate.metamodel.source.hbm.state.domain;

import java.util.Map;

import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.binding.MappingDefaults;
import org.hibernate.metamodel.domain.Attribute;
import org.hibernate.metamodel.domain.MetaAttribute;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.metamodel.source.util.MappingHelper;
import org.hibernate.metamodel.state.domain.AttributeDomainState;

/**
 * @author Gail Badner
 */
public abstract class AbstractHbmAttributeDomainState implements AttributeDomainState {
	private final MetadataImpl metadata;
	private final MappingDefaults defaults;
	private final Attribute attribute;
	private final String nodeName;
	private final String accessorName;
	private final boolean isOptimisticLockable;
	private final Map<String, MetaAttribute> metaAttributes;

	public AbstractHbmAttributeDomainState(
			MetadataImpl metadata,
			MappingDefaults defaults,
			Attribute attribute,
			String nodeName,
			Map<String, MetaAttribute> metaAttributes,
			String accessorName,
			boolean isOptimisticLockable) {
		this.metadata = metadata;
		this.defaults = defaults;
		this.attribute = attribute;
		this.nodeName = MappingHelper.getStringValue( nodeName, attribute.getName() );
		this.metaAttributes = metaAttributes;
		this.accessorName = accessorName;
		this.isOptimisticLockable = isOptimisticLockable;
	}

	public MetadataImpl getMetadata() {
		return metadata;
	}

	protected final MappingDefaults getDefaults() {
		return defaults;
	}

	public final Attribute getAttribute() {
		return attribute;
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
		return nodeName;
	}

	public final Map<String, MetaAttribute> getMetaAttributes() {
		return metaAttributes;
	}
}
