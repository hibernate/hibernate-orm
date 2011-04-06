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
package org.hibernate.metamodel.source.hbm.state.relational;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.dom4j.Attribute;
import org.dom4j.Element;

import org.hibernate.MappingException;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.metamodel.binding.MappingDefaults;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.source.util.DomHelper;

/**
 * @author Gail Badner
 */
public class HbmSimpleValueRelationalStateContainer extends HbmRelationalState implements SimpleAttributeBinding.TupleRelationalState {
	private final MappingDefaults defaults;
	private final Set<String> propertyUniqueKeys;
	private final Set<String> propertyIndexes;
	private final LinkedHashSet<SimpleAttributeBinding.SingleValueRelationalState> singleValueStates =
			new LinkedHashSet<SimpleAttributeBinding.SingleValueRelationalState>();

	public NamingStrategy getNamingStrategy() {
		return defaults.getNamingStrategy();
	}

	public HbmSimpleValueRelationalStateContainer(MappingDefaults defaults,
												  Element propertyElement,
												  boolean autoColumnCreation) {
		super( propertyElement );
		this.defaults = defaults;
		this.propertyUniqueKeys = DomHelper.extractUniqueAttributeValueTokens( propertyElement, "unique-key", ", " );
		this.propertyIndexes = DomHelper.extractUniqueAttributeValueTokens( propertyElement, "index", ", " );
		final Attribute columnAttribute = getElement().attribute( "column" );
		if ( columnAttribute == null ) {
			final Iterator valueElements = getElement().elementIterator();
			while ( valueElements.hasNext() ) {
				final Element valueElement = (Element) valueElements.next();
				if ( "column".equals( valueElement.getName() ) ) {
					singleValueStates.add( new HbmColumnRelationalState( valueElement, this  ) );
				}
				else if ( "formula".equals( valueElement.getName() ) ) {
					singleValueStates.add( new HbmDerivedValueRelationalState( valueElement, this ) );
				}
			}
		}
		else {
			if ( propertyElement.elementIterator( "column" ).hasNext() ) {
				throw new MappingException( "column attribute may not be used together with <column> subelement" );
			}
			if ( propertyElement.elementIterator( "formula" ).hasNext() ) {
				throw new MappingException( "column attribute may not be used together with <formula> subelement" );
			}
			singleValueStates.add( new HbmColumnRelationalState( propertyElement, this ) );
		}
		// TODO: should it actually check for 0 columns???
		if ( singleValueStates.isEmpty() && autoColumnCreation ) {
			singleValueStates.add( new HbmColumnRelationalState( propertyElement, this ) );
		}
	}

	public LinkedHashSet<SimpleAttributeBinding.SingleValueRelationalState> getSingleValueRelationalStates() {
		return singleValueStates;
	}

	Set<String> getPropertyUniqueKeys() {
		return propertyUniqueKeys;
	}

	Set<String> getPropertyIndexes() {
		return propertyIndexes;
	}
}
