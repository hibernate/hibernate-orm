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

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.spi.PluralAttributeIndexNature;
import org.hibernate.metamodel.spi.relational.Value;

/**
 *
 */
public class BasicPluralAttributeIndexBinding extends AbstractPluralAttributeIndexBinding {

	private RelationalValueBindingContainer relationalValueBindingContainer;

	public BasicPluralAttributeIndexBinding(
			IndexedPluralAttributeBinding pluralAttributeBinding) {
		super( pluralAttributeBinding );
	}

	@Override
	public List<RelationalValueBinding> getRelationalValueBindings() {
		return relationalValueBindingContainer.relationalValueBindings();
	}

	@Override
	public List<Value> getValues() {
		return relationalValueBindingContainer.values();
	}

	public void setRelationalValueBindings(List<RelationalValueBinding> relationalValueBindings) {
		if ( relationalValueBindings == null || relationalValueBindings.isEmpty() ) {
			throw new AssertionFailure( "relationalValueBindings argument must be non-null and non-empty." );
		}
		if ( this.relationalValueBindingContainer != null ) {
			throw new AssertionFailure( "Relational value bindings have already initialized" );
		}
		this.relationalValueBindingContainer =
				new RelationalValueBindingContainer( relationalValueBindings );
	}

	@Override
	public PluralAttributeIndexNature getNature() {
		return PluralAttributeIndexNature.BASIC;
	}
}
