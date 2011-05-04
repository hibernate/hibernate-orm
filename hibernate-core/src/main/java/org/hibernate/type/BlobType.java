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
package org.hibernate.type;
import java.sql.Blob;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.descriptor.java.BlobTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#BLOB BLOB} and {@link Blob}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class BlobType extends AbstractSingleColumnStandardBasicType<Blob> {
	public static final BlobType INSTANCE = new BlobType();

	public BlobType() {
		super( org.hibernate.type.descriptor.sql.BlobTypeDescriptor.DEFAULT, BlobTypeDescriptor.INSTANCE );
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return "blob";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	protected Blob getReplacement(Blob original, Blob target, SessionImplementor session) {
		return session.getFactory().getDialect().getLobMergeStrategy().mergeBlob( original, target, session );
	}

}

