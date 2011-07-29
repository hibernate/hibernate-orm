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
package org.hibernate.metamodel.binding;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.relational.ForeignKey;
import org.hibernate.metamodel.relational.TableSpecification;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class CollectionKey {
	private final AbstractPluralAttributeBinding pluralAttributeBinding;

	private ForeignKey foreignKey;
	private boolean inverse;
	private HibernateTypeDescriptor hibernateTypeDescriptor;

// todo : this would be nice to have but we do not always know it, especially in HBM case.
//	private BasicAttributeBinding otherSide;

	public CollectionKey(AbstractPluralAttributeBinding pluralAttributeBinding) {
		this.pluralAttributeBinding = pluralAttributeBinding;
	}

	public AbstractPluralAttributeBinding getPluralAttributeBinding() {
		return pluralAttributeBinding;
	}

	public void prepareForeignKey(String foreignKeyName, String targetTableName) {
		if ( foreignKey != null ) {
			throw new AssertionFailure( "Foreign key already initialized" );
		}
		final TableSpecification collectionTable = pluralAttributeBinding.getCollectionTable();
		if ( collectionTable == null ) {
			throw new AssertionFailure( "Collection table not yet bound" );
		}

		final TableSpecification targetTable = pluralAttributeBinding.getContainer()
				.seekEntityBinding()
				.locateTable( targetTableName );

		// todo : handle implicit fk names...

		foreignKey = collectionTable.createForeignKey( targetTable, foreignKeyName );
	}

	public ForeignKey getForeignKey() {
		return foreignKey;
	}
}
