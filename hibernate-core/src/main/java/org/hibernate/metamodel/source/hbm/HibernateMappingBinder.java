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
package org.hibernate.metamodel.source.hbm;

import java.util.List;

import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.source.spi.Binder;
import org.hibernate.metamodel.source.spi.MetadataImplementor;

/**
 * @author Steve Ebersole
 */
public class HibernateMappingBinder implements Binder {
	private final MetadataImplementor metadata;

	public HibernateMappingBinder(MetadataImplementor metadata) {
		this.metadata = metadata;
	}

	@Override
	public void prepare(MetadataSources sources) {
		// nothing to do here.
	}

	@Override
	public void bindIndependentMetadata(MetadataSources sources) {
		// todo : implement method body
	}

	@Override
	public void bindTypeDependentMetadata(MetadataSources sources) {
		// todo : implement method body
	}

	@Override
	public void bindMappingMetadata(MetadataSources sources, List<String> processedEntityNames) {
		// todo : implement method body
	}

	@Override
	public void bindMappingDependentMetadata(MetadataSources sources) {
		// todo : implement method body
	}
}
