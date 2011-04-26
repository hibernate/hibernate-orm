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

import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.metamodel.relational.ForeignKey;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.source.Metadata;

/**
 * TODO : javadoc
 *
 * @author Gail Badner
 */
public class ManyToOneAttributeBinding extends SingularAttributeBinding implements EntityReferencingAttributeBinding {
	private boolean isLogicalOneToOne;
	private boolean isPropertyReference;
	private String foreignKeyName;
	private String referencedAttributeName;
	private String referencedEntityName;
	private AttributeBinding referencedAttributeBinding;
	private boolean ignoreNotFound;

	public static interface DomainState extends SingularAttributeBinding.DomainState {
		boolean isUnwrapProxy();
		String getReferencedAttributeName();
		String getReferencedEntityName();
		boolean ignoreNotFound();
	}

	public static interface RelationalState extends SingleValueRelationalState {
		boolean isLogicalOneToOne();
		String getForeignKeyName();
	}

	ManyToOneAttributeBinding(EntityBinding entityBinding) {
		super( entityBinding, false, false );
	}

	public final void initialize(DomainState state) {
		super.initialize( state );
		isPropertyReference = state.getReferencedAttributeName() != null;
		referencedAttributeName = state.getReferencedAttributeName();
		referencedEntityName = state.getReferencedEntityName();
	}

	public final void initialize(RelationalState state) {
		super.initializeSingleValue( state );
		isLogicalOneToOne = state.isLogicalOneToOne();
		foreignKeyName = state.getForeignKeyName();
	}

	public final String getReferencedEntityName() {
		return referencedEntityName;
	}

	public final String getReferencedAttributeName() {
		if ( referencedAttributeName == null ) {
			throw new IllegalStateException(
				"Referenced attribute name is not available."
			);
		}
		return referencedAttributeName;
	}

	public final boolean isReferenceResolved() {
		return referencedAttributeBinding != null;
	}

	public final EntityBinding getReferencedEntityBinding() {
		if ( ! isReferenceResolved() ) {
			throw new IllegalStateException( "EntityBinding reference has not be referenced." );
		}
		// TODO: throw exception if referencedEntityBinding is null?
		return referencedAttributeBinding.getEntityBinding();
	}

	public final void resolveReference(AttributeBinding referencedAttributeBinding) {
		if ( ! referencedEntityName.equals( referencedAttributeBinding.getEntityBinding().getEntity().getName() ) ) {
			throw new IllegalStateException(
					"attempt to set EntityBinding with name: [" +
							referencedAttributeBinding.getEntityBinding().getEntity().getName() +
							"; entity name should be: " + referencedEntityName
			);
		}
		if ( referencedAttributeName == null ) {
			referencedAttributeName = referencedAttributeBinding.getAttribute().getName();
		}
		else if ( ! referencedAttributeName.equals( referencedAttributeBinding.getAttribute().getName() ) ) {
			throw new IllegalStateException(
					"Inconsistent attribute name; expected: " + referencedAttributeName +
					"actual: " + referencedAttributeBinding.getAttribute().getName()
			);
		}
		this.referencedAttributeBinding = referencedAttributeBinding;
		buildForeignKey();
	}

	private void buildForeignKey() {
		// TODO: move this stuff to relational model
		ForeignKey foreignKey = getTable().createForeignKey(  referencedAttributeBinding.getTable(), foreignKeyName );
		Iterator<SimpleValue> referencingValueIterator = getValues().iterator();
		Iterator<SimpleValue> targetValueIterator =  referencedAttributeBinding.getValues().iterator();
		while ( referencingValueIterator.hasNext() ) {
			if ( ! targetValueIterator.hasNext() ) {
				// TODO: improve this message
				throw new MappingException( "number of values in many-to-one reference is greater than number of values in target" );
			}
			SimpleValue referencingValue = referencingValueIterator.next();
			SimpleValue targetValue = targetValueIterator.next();
			if ( Column.class.isInstance( referencingValue ) ) {
				if ( ! Column.class.isInstance( targetValue ) ) {
					// TODO improve this message
					throw new MappingException( "referencing value is a column, but target is not a column" );
				}
				foreignKey.addColumnMapping( Column.class.cast( referencingValue ), Column.class.cast( targetValue ) );
			}
			else if ( Column.class.isInstance( targetValue ) ) {
				// TODO: improve this message
				throw new MappingException( "referencing value is not a column, but target is a column." );
			}
		}
		if ( targetValueIterator.hasNext() ) {
			throw new MappingException( "target value has more simple values than referencing value" );
		}
	}

	public boolean isSimpleValue() {
		return false;
	}

	public void validate() {
		// can't check this until both the domain and relational states are initialized...
		if ( getCascade() != null && getCascade().indexOf( "delete-orphan" ) >= 0 ) {
			if ( ! isLogicalOneToOne ) {
				throw new MappingException(
						"many-to-one attribute [" + getAttribute().getName() + "] does not support orphan delete as it is not unique"
				);
			}
		}
		//TODO: validate that the entity reference is resolved
	}
}