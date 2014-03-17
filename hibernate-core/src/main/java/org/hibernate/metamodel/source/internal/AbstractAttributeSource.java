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
package org.hibernate.metamodel.source.internal;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.metamodel.source.internal.annotations.attribute.AbstractPersistentAttribute;
import org.hibernate.metamodel.source.spi.AttributeSource;
import org.hibernate.metamodel.source.spi.ToolingHintSource;

/**
 * Common contract for source of all persistent attributes.
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public abstract class AbstractAttributeSource implements AttributeSource {
	private final AbstractManagedTypeSource container;
	private final AbstractPersistentAttribute attribute;

	protected AbstractAttributeSource(
			AbstractManagedTypeSource container,
			AbstractPersistentAttribute attribute) {
		this.container = container;
		this.attribute = attribute;
	}

	public AbstractManagedTypeSource getContainer() {
		return container;
	}

	public AbstractPersistentAttribute getPersistentAttribute() {
		return attribute;
	}

	@Override
	public String getName() {
		return attribute.getName();
	}

	@Override
	public String getPropertyAccessorName() {
		return attribute.getAccessorStrategy();
	}

	@Override
	public boolean isIncludedInOptimisticLocking() {
		return getPersistentAttribute().isIncludeInOptimisticLocking();
	}

	@Override
	public Collection<? extends ToolingHintSource> getToolingHintSources() {
		// todo : when hooking in unified (Hibernate-specific) xml elements need to account for this
		return Collections.emptyList();
	}
}
