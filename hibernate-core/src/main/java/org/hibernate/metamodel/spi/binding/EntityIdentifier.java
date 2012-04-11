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
import java.util.Properties;

import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.metamodel.spi.relational.Column;

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

	private BoundType boundType;

	private IdGenerator idGenerator;
	private String unsavedValue;

	private Class idClassClass; // the class named in @IdClass

	private IdentifierGenerator identifierGenerator;

	private SingularNonAssociationAttributeBinding attributeBinding;
	private List<SingularAttributeBinding> nonAggregatedCompositeAttributeBindings;

	/**
	 * Create an identifier
	 *
	 * @param entityBinding the entity binding for which this instance is the id
	 */
	public EntityIdentifier(EntityBinding entityBinding) {
		this.entityBinding = entityBinding;
	}

	public void bindAsSingleAttributeIdentifier(
			SingularNonAssociationAttributeBinding attributeBinding,
			IdGenerator idGenerator,
			String unsavedValue) {
		if ( boundType != null ) {
			throw new IllegalStateException( "Entity identifier was already bound" );
		}
		this.boundType = BoundType.SINGLE_ATTRIBUTE;
		this.attributeBinding = attributeBinding;
		this.idGenerator = idGenerator;
		this.unsavedValue = unsavedValue;

		// Configure primary key in relational model
		for ( final RelationalValueBinding valueBinding : attributeBinding.getRelationalValueBindings() ) {
			entityBinding.getPrimaryTable().getPrimaryKey().addColumn( (Column) valueBinding.getValue() );
		}
	}

	public void bindAsMultipleAttributeIdentifier(
			List<SingularAttributeBinding> nonAggregatedCompositeAttributeBindings,
			Class idClassClass) {
		if ( boundType != null ) {
			throw new IllegalStateException( "Entity identifier was already bound" );
		}
		this.boundType = BoundType.MULTIPLE_ATTRIBUTE;
		this.nonAggregatedCompositeAttributeBindings = nonAggregatedCompositeAttributeBindings;
		this.idClassClass = idClassClass;
	}

	public SingularNonAssociationAttributeBinding getValueBinding() {
		return attributeBinding;
	}

	public List<SingularAttributeBinding> getNonAggregatedCompositeAttributeBindings() {
		return nonAggregatedCompositeAttributeBindings;
	}

	public String getUnsavedValue() {
		return unsavedValue;
	}

	public boolean isEmbedded() {
		return boundType == BoundType.SINGLE_ATTRIBUTE && attributeBinding.getRelationalValueBindings().size() > 1;
	}

	public Class getIdClassClass() {
		return idClassClass;
	}

	public boolean isIdentifierMapper() {
		// i think
		return boundType == BoundType.MULTIPLE_ATTRIBUTE && idClassClass != null;
	}

	// todo do we really need this createIdentifierGenerator and how do we make sure the getter is not called too early
	// maybe some sort of visitor pattern here!? (HF)
	public IdentifierGenerator createIdentifierGenerator(IdentifierGeneratorFactory factory, Properties properties) {
		if ( idGenerator != null ) {
			identifierGenerator = attributeBinding.createIdentifierGenerator( idGenerator, factory, properties );
		}
		return identifierGenerator;
	}

	public IdentifierGenerator getIdentifierGenerator() {
		return identifierGenerator;
	}

	private static enum BoundType {
		SINGLE_ATTRIBUTE,
		MULTIPLE_ATTRIBUTE
	}
}
