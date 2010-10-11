/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.engine.jdbc.batch.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.spi.LogicalConnectionImplementor;

/**
 * A builder for {@link Batch} instances.
 *
 * @author Steve Ebersole
 */
public class BatchBuilder {
	private static final Logger log = LoggerFactory.getLogger( BatchBuilder.class );

	private int size;

	public BatchBuilder() {
	}

	public BatchBuilder(int size) {
		this.size = size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public Batch buildBatch(Object key, LogicalConnectionImplementor logicalConnection) {
		log.trace( "building batch [size={}]", size );
		return size > 1
				? new BatchingBatch( key, logicalConnection, size )
				: new NonBatchingBatch( key, logicalConnection );
	}
}

