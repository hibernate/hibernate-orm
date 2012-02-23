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
package org.hibernate.metamodel.spi.binding;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.TableSpecification;

/**
 * Describes the binding information pertaining to the plural attribute foreign key.
 *
 * @author Steve Ebersole
 */
public class PluralAttributeKeyBinding {
	private final AbstractPluralAttributeBinding pluralAttributeBinding;

	private ForeignKey foreignKey;
	private boolean inverse;
	// may need notion of "boolean updatable"

	// this knowledge can be implicitly resolved based on the typing information on the referenced owner attribute
	private final HibernateTypeDescriptor hibernateTypeDescriptor = new HibernateTypeDescriptor();
	private final SingularAttributeBinding referencedAttributeBinding;

// todo : this would be nice to have but we do not always know it, especially in HBM case.
//	private BasicAttributeBinding otherSide;


	public PluralAttributeKeyBinding(
			AbstractPluralAttributeBinding pluralAttributeBinding,
			SingularAttributeBinding referencedAttributeBinding) {
		this.pluralAttributeBinding = pluralAttributeBinding;
		this.referencedAttributeBinding = referencedAttributeBinding;
	}

	/**
	 * Identifies the plural attribute binding whose foreign key this class is describing.
	 *
	 * @return The plural attribute whose foreign key is being described
	 */
	public AbstractPluralAttributeBinding getPluralAttributeBinding() {
		return pluralAttributeBinding;
	}

	public SingularAttributeBinding getReferencedAttributeBinding() {
		return referencedAttributeBinding;
	}
	/**
	 * The foreign key that defines the scope of this relationship.
	 *
	 * @return The foreign key being bound to.
	 */
	public ForeignKey getForeignKey() {
		return foreignKey;
	}

	/**
	 * Is the plural attribute considered inverse?
	 * <p/>
	 * NOTE: The "inverse-ness" of a plural attribute logically applies to it key.
	 *
	 * @return {@code true} indicates the plural attribute is inverse; {@code false} indicates is not.
	 */
	public boolean isInverse() {
		return inverse;
	}

	public void setInverse(boolean inverse) {
		this.inverse = inverse;
	}

	public HibernateTypeDescriptor getHibernateTypeDescriptor() {
		return hibernateTypeDescriptor;
	}

	public void prepareForeignKey(String foreignKeyName, TableSpecification targetTable) {
		if ( foreignKey != null ) {
			throw new AssertionFailure( "Foreign key already initialized" );
		}
		final TableSpecification collectionTable = pluralAttributeBinding.getCollectionTable();
		if ( collectionTable == null ) {
			throw new AssertionFailure( "Collection table not yet bound" );
		}

		if ( foreignKeyName != null ) {
			foreignKey = collectionTable.locateForeignKey( foreignKeyName );
			if ( foreignKey != null ) {
				return;
			}
		}

		foreignKey = collectionTable.createForeignKey( targetTable, foreignKeyName );
	}

}
