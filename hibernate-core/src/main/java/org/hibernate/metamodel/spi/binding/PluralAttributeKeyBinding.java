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

import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.Value;

/**
 * Describes the binding information pertaining to the plural attribute foreign key.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class PluralAttributeKeyBinding {
	private final AbstractPluralAttributeBinding pluralAttributeBinding;
	private final SingularAttributeBinding referencedAttributeBinding;
	private JoinRelationalValueBindingContainer relationalValueBindingContainer;
	private boolean inverse;

	// this knowledge can be implicitly resolved based on the typing information on the referenced owner attribute
	private HibernateTypeDescriptor hibernateTypeDescriptor = new HibernateTypeDescriptor();


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

	public TableSpecification getCollectionTable() {
		return relationalValueBindingContainer.getTable();
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

	public void setHibernateTypeDescriptor(HibernateTypeDescriptor hibernateTypeDescriptor) {
		this.hibernateTypeDescriptor = hibernateTypeDescriptor;
	}

	public List<RelationalValueBinding> getRelationalValueBindings() {
		return relationalValueBindingContainer.relationalValueBindings();
	}

	public List<Value> getValues() {
		return relationalValueBindingContainer.values();
	}
	public ForeignKey getForeignKey() {
		return relationalValueBindingContainer.getForeignKey();
	}
	public void setJoinRelationalValueBindings(
			List<RelationalValueBinding> relationalValueBindings,
			ForeignKey foreignKey) {
		if ( relationalValueBindings == null || relationalValueBindings.isEmpty() ) {
			throw new AssertionFailure( "relationalValueBindings argument must be non-null and non-empty." );
		}
		if ( this.relationalValueBindingContainer != null ) {
			throw new AssertionFailure( "Relational value bindings have already initialized" );
		}
		this.relationalValueBindingContainer = new JoinRelationalValueBindingContainer(
				relationalValueBindings,
				foreignKey
		);
		if ( this.relationalValueBindingContainer.hasDerivedValue() ) {
				throw new NotYetImplementedException(
						"Derived values are not supported when creating a foreign key that targets columns."
				);
		}
	}

	public boolean isCascadeDeleteEnabled() {
		return  relationalValueBindingContainer.getForeignKey().getDeleteRule() == ForeignKey.ReferentialAction.CASCADE;
	}

	public boolean isNullable() {
		// cannot be nullable if the foreign key source columns are included in the primary key
		// TODO: move this into RelationalValueBindingContainer.
		if ( getCollectionTable().getPrimaryKey().getColumns().containsAll( relationalValueBindingContainer.columns() ) ) {
			return false;
		}
		return relationalValueBindingContainer.hasNullableRelationalValueBinding();
	}

	public boolean isInsertable() {
		return relationalValueBindingContainer.hasInsertableRelationalValueBinding();
	}

	public boolean isUpdatable() {
		return relationalValueBindingContainer.hasUpdateableRelationalValueBinding();
	}
}
