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

import java.util.Properties;

import org.hibernate.AssertionFailure;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.metamodel.spi.relational.Column;

import static org.hibernate.id.EntityIdentifierNature.AGGREGATED_COMPOSITE;
import static org.hibernate.id.EntityIdentifierNature.COMPOSITE;
import static org.hibernate.id.EntityIdentifierNature.SIMPLE;

/**
 * Hold information about the entity identifier.  At a high-level, can be one of 2-types:<ul>
 *     <li>single-attribute identifier - this includes both simple identifiers and aggregated composite identifiers</li>
 *     <li>multiple-attribute identifier - non-aggregated composite identifiers</li>
 * </ul>
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class EntityIdentifier {
	private final EntityBinding entityBinding;
	private EntityIdentifierNature nature;

	private SingularNonAssociationAttributeBinding identifierAttributeBinding;

	private IdGenerator idGenerator;
	private String unsavedValue;

	private Class idClassClass; // the class named in @IdClass

	private IdentifierGenerator identifierGenerator;
	private int columnCount;

	/**
	 * Create an identifier
	 *
	 * @param entityBinding the entity binding for which this instance is the id
	 */
	public EntityIdentifier(EntityBinding entityBinding) {
		this.entityBinding = entityBinding;
	}

	public void prepareAsSimpleIdentifier(
			SingularNonAssociationAttributeBinding attributeBinding,
			IdGenerator idGenerator,
			String unsavedValue) {
		prepareAsSingleAttributeIdentifier( SIMPLE, attributeBinding, idGenerator, unsavedValue );
	}

	public void prepareAsAggregatedCompositeIdentifier(
			SingularNonAssociationAttributeBinding attributeBinding,
			IdGenerator idGenerator,
			String unsavedValue) {
		prepareAsSingleAttributeIdentifier( AGGREGATED_COMPOSITE, attributeBinding, idGenerator, unsavedValue );
	}

	protected void prepareAsSingleAttributeIdentifier(
			EntityIdentifierNature nature,
			SingularNonAssociationAttributeBinding attributeBinding,
			IdGenerator idGenerator,
			String unsavedValue) {
		ensureNotBound();

		this.nature = nature;
		this.identifierAttributeBinding = attributeBinding;
		this.idGenerator = idGenerator;
		this.unsavedValue = unsavedValue;
		this.columnCount = attributeBinding.getRelationalValueBindings().size();

		// Configure primary key in relational model
		for ( final RelationalValueBinding valueBinding : attributeBinding.getRelationalValueBindings() ) {
			entityBinding.getPrimaryTable().getPrimaryKey().addColumn( (Column) valueBinding.getValue() );
		}
	}

	public void prepareAsNonAggregatedCompositeIdentifier(
			CompositeAttributeBinding syntheticCompositeAttributeBinding,
			Class idClassClass) {
		ensureNotBound();

		this.nature = COMPOSITE;
		this.identifierAttributeBinding = syntheticCompositeAttributeBinding;
		this.idClassClass = idClassClass;

		for ( AttributeBinding attributeBinding : syntheticCompositeAttributeBinding.attributeBindings() ) {
			if ( ! attributeBinding.getAttribute().isSingular() ) {
				throw new AssertionFailure( "Expecting all singular attribute bindings as part of composite identifier" );
			}
			final SingularAttributeBinding singularAttributeBinding = (SingularAttributeBinding) attributeBinding;
			columnCount += singularAttributeBinding.getRelationalValueBindings().size();

			// Configure primary key in relational model
			for ( final RelationalValueBinding valueBinding : singularAttributeBinding.getRelationalValueBindings() ) {
				entityBinding.getPrimaryTable().getPrimaryKey().addColumn( (Column) valueBinding.getValue() );
			}
		}
	}

	public EntityIdentifierNature getNature() {
		return nature;
	}

	public boolean isSingleAttribute() {
		ensureBound();
		return getNature() != COMPOSITE;
	}

	public SingularNonAssociationAttributeBinding getAttributeBinding() {
		ensureBound();
		return identifierAttributeBinding;
	}

	public boolean isIdentifierAttributeBinding(AttributeBinding attributeBinding) {
		ensureBound();
		if ( getNature() == COMPOSITE ) {
			for ( AttributeBinding identifierAttributeBinding
					: ( (CompositeAttributeBinding) this.identifierAttributeBinding ).attributeBindings() ) {
				if ( identifierAttributeBinding.equals( attributeBinding ) ) {
					return true;
				}
			}
			return false;
		}
		else {
			return identifierAttributeBinding.equals( attributeBinding );
		}
	}

	public String getUnsavedValue() {
		return unsavedValue;
	}

	public boolean isEmbedded() {
		return isSingleAttribute() && columnCount > 1;
	}

	public Class getIdClassClass() {
		return idClassClass;
	}

	public boolean isIdentifierMapper() {
		// i think this is the intended check for this method
		return ! isSingleAttribute() && idClassClass != null;
	}

	// todo do we really need this createIdentifierGenerator and how do we make sure the getter is not called too early
	// maybe some sort of visitor pattern here!? (HF)
	public IdentifierGenerator createIdentifierGenerator(IdentifierGeneratorFactory factory, Properties properties) {
		ensureBound();
		if ( identifierGenerator == null ) {
			if ( isSingleAttribute() && idGenerator != null ) {
				identifierGenerator = identifierAttributeBinding.createIdentifierGenerator(
						idGenerator,
						factory,
						properties
				);
			}
		}
		return identifierGenerator;
	}

	public IdentifierGenerator getIdentifierGenerator() {
		ensureBound();
		return identifierGenerator;
	}

	protected void ensureBound() {
		if ( ! isBound() ) {
			throw new IllegalStateException( "Entity identifier was not yet bound" );
		}
	}

	protected void ensureNotBound() {
		if ( isBound() ) {
			throw new IllegalStateException( "Entity identifier was already bound" );
		}
	}

	protected boolean isBound() {
		return nature != null;
	}

	public int getColumnCount() {
		ensureBound();
		return columnCount;
	}
}
