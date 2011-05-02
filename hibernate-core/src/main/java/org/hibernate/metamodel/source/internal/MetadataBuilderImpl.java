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
package org.hibernate.metamodel.source.internal;

import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.MetadataBuilder;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.SourceProcessingOrder;

/**
 * @author Steve Ebersole
 */
public class MetadataBuilderImpl implements MetadataBuilder {
	private final MetadataSources sources;

	private NamingStrategy namingStrategy = EJB3NamingStrategy.INSTANCE;
	private SourceProcessingOrder sourceProcessingOrder = SourceProcessingOrder.HBM_FIRST;

	public MetadataBuilderImpl(MetadataSources sources) {
		this.sources = sources;
	}

	public MetadataSources getSources() {
		return sources;
	}

	public NamingStrategy getNamingStrategy() {
		return namingStrategy;
	}

	public SourceProcessingOrder getSourceProcessingOrder() {
		return sourceProcessingOrder;
	}

	@Override
	public MetadataBuilder with(NamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
		return this;
	}

	@Override
	public MetadataBuilder with(SourceProcessingOrder sourceProcessingOrder) {
		this.sourceProcessingOrder = sourceProcessingOrder;
		return this;
	}

	@Override
	public Metadata buildMetadata() {
		return new MetadataImpl( this );
	}
}
