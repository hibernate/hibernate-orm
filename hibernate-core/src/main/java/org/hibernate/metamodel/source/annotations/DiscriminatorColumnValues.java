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
package org.hibernate.metamodel.source.annotations;

/**
 * Container for the properties of a discriminator column.
 *
 * @author Hardy Ferentschik
 */
public class DiscriminatorColumnValues extends ColumnValues {
	private static final String DEFAULT_DISCRIMINATOR_COLUMN_NAME = "DTYPE";
	private static final String DEFAULT_DISCRIMINATOR_TYPE = "string";
	private static final int DEFAULT_DISCRIMINATOR_LENGTH = 31;

	public DiscriminatorColumnValues() {
		super();
		setName( DEFAULT_DISCRIMINATOR_COLUMN_NAME );
		setLength( DEFAULT_DISCRIMINATOR_LENGTH );
		setNullable( false );
//		if ( columnAnnotation != null && !JPADotNames.COLUMN.equals( columnAnnotation.name() ) ) {
//			throw new AssertionFailure( "A @Column annotation needs to be passed to the constructor" );
//		}
//		applyColumnValues( columnAnnotation, isId );
	}
}


