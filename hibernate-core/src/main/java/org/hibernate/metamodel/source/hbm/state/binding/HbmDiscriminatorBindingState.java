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

import org.hibernate.metamodel.source.hbm.MappingDefaults;
import org.hibernate.metamodel.binding.state.DiscriminatorBindingState;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping.XMLClass.XMLDiscriminator;

/**
 * @author Gail Badner
 */
public class HbmDiscriminatorBindingState extends AbstractHbmAttributeBindingState
		implements DiscriminatorBindingState {
	private final XMLDiscriminator discriminator;

	public HbmDiscriminatorBindingState(
			String ownerClassName,
			MappingDefaults defaults,
			XMLDiscriminator discriminator) {
		// Discriminator.getName() is not defined, so the attribute will always be
		// defaults.getDefaultDescriminatorColumnName()
		super(
				ownerClassName, defaults.getDefaultDiscriminatorColumnName(), defaults, null, null, null, true
		);
		this.discriminator = discriminator;
	}

	public String getCascade() {
		return null;
	}

	protected boolean isEmbedded() {
		return false;
	}

	public String getTypeName() {
		return discriminator.getType() == null ? "string" : discriminator.getType();
	}

	public boolean isLazy() {
		return false;
	}

	public boolean isInsertable() {
		return discriminator.isInsert();
	}

	public boolean isUpdatable() {
		return false;
	}

	public boolean isForced() {
		return discriminator.isForce();
	}
}
