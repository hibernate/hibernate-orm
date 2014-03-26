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
package org.hibernate.metamodel.spi.binding;

import java.util.List;

import org.hibernate.metamodel.spi.NaturalIdMutability;
import org.hibernate.metamodel.spi.domain.SingularAttribute;
import org.hibernate.metamodel.spi.relational.Value;

/**
 * Specialized binding contract for singular (non-collection) attributes
 *
 * @author Steve Ebersole
 */
public interface SingularAttributeBinding extends AttributeBinding {

	@Override
	public SingularAttribute getAttribute();

	public List<RelationalValueBinding> getRelationalValueBindings();

	public List<Value> getValues();
	/**
	 * Convenience method to determine if any {@link RelationalValueBinding simple value bindings} are derived values
	 * (formula mappings).
	 *
	 * @return {@code true} indicates that the binding contains a derived value; {@code false} indicates it does not.
	 */
	public boolean hasDerivedValue();

	/**
	 * Convenience method to determine if all {@link RelationalValueBinding simple value bindings} allow nulls.
	 *
	 * @return {@code true} indicates that all values allow {@code null}; {@code false} indicates one or more do not.
	 */
	public boolean isNullable();

	/**
	 * Convenience method to determine if all tables (primary and secondary) involved with this attribute
	 * binding are optional.
	 *
	 * @return true, if all tables involved with this attribute are optional; false, otherwise.
	 */
	public boolean isOptional();

	/**
	 * Convenience method to determine if any {@link RelationalValueBinding simple value bindings} are inserted.
	 *
	 * @return {@code true} indicates that at least one value is inserted; {@code false} indicates none are inserted.
	 */
	public boolean isIncludedInInsert();

	/**
	 * Convenience method to determine if any {@link RelationalValueBinding simple value bindings} can be updated.
	 *
	 * @return {@code true} indicates that at least one value can be updated; {@code false} indicates none can
	 * be updated.
	 */
	public boolean isIncludedInUpdate();

	/**
	 * Convenience method to determine if this attribute is an natural id and if it is, then returns its mutability.
	 *
	 * @return The {@link org.hibernate.metamodel.spi.NaturalIdMutability} linked with this attribute,
	 * {@code NaturalIdMutability#NOT_NATURAL_ID} indicates this is <b>NOT</b> a natural id attribute.
	 */
	public NaturalIdMutability getNaturalIdMutability();

	public void setAlternateUniqueKey(boolean alternateUniqueKey);

}
