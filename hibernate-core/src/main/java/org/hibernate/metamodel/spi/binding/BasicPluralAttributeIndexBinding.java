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

import org.hibernate.metamodel.spi.domain.IndexedPluralAttribute;
import org.hibernate.metamodel.spi.domain.Type;
import org.hibernate.metamodel.spi.relational.Value;

/**
 *
 */
public class BasicPluralAttributeIndexBinding implements PluralAttributeIndexBinding {

	private final IndexedPluralAttributeBinding pluralAttributeBinding;
	private final PluralAttributeIndexNature pluralAttributeIndexNature;
	private final HibernateTypeDescriptor hibernateTypeDescriptor = new HibernateTypeDescriptor();
	private Value value;

	public BasicPluralAttributeIndexBinding(
			IndexedPluralAttributeBinding pluralAttributeBinding,
			PluralAttributeIndexNature pluralAttributeIndexNature) {
		this.pluralAttributeBinding = pluralAttributeBinding;
		this.pluralAttributeIndexNature = pluralAttributeIndexNature;
	}

	@Override
	public HibernateTypeDescriptor getHibernateTypeDescriptor() {
		return hibernateTypeDescriptor;
	}

	@Override
	public Value getIndexRelationalValue() {
		return value;
	}

	@Override
	public IndexedPluralAttributeBinding getPluralAttributeBinding() {
		return pluralAttributeBinding;
	}

	public void setIndexRelationalValue( Value value ) {
		this.value = value;
	}

	@Override
	public Type getPluralAttributeIndexType() {
		return ( (IndexedPluralAttribute) getPluralAttributeBinding().getAttribute() ).getIndexType();
	}

	@Override
	public PluralAttributeIndexNature getPluralAttributeIndexNature() {
		return pluralAttributeIndexNature;
	}
}
