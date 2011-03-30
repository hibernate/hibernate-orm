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
package org.hibernate.metamodel.source.hbm.state.domain;

import org.dom4j.Element;

import org.hibernate.MappingException;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.binding.MappingDefaults;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.source.util.DomHelper;

/**
 * @author Gail Badner
 */
public class HbmSimpleAttributeDomainState extends AbstractHbmAttributeDomainState implements SimpleAttributeBinding.DomainState {
	public HbmSimpleAttributeDomainState(MappingDefaults defaults,
										 final Element element,
										 org.hibernate.metamodel.domain.Attribute attribute) {
		super( defaults, element, attribute );
	}

	protected boolean isEmbedded() {
		return false;
	}

	public boolean isLazy() {
		return DomHelper.extractBooleanAttributeValue( getElement(), "lazy", false );
	}

	public PropertyGeneration getPropertyGeneration() {
		return PropertyGeneration.parse( DomHelper.extractAttributeValue( getElement(), "generated", null ) );
	}
	public boolean isInsertable() {
		//TODO: implement
		PropertyGeneration generation = getPropertyGeneration();
		boolean isInsertable = DomHelper.extractBooleanAttributeValue( getElement(), "insert", true );
		if ( generation == PropertyGeneration.ALWAYS || generation == PropertyGeneration.INSERT ) {
			// generated properties can *never* be insertable...
			if ( isInsertable ) {
				final org.dom4j.Attribute insertAttribute = getElement().attribute( "insert" );
				if ( insertAttribute == null ) {
					// insertable simply because the user did not specify anything; just override it
					isInsertable = false;
				}
				else {
					// the user specifically supplied insert="true", which constitutes an illegal combo
					throw new MappingException(
							"cannot specify both insert=\"true\" and generated=\"" + generation.getName() +
							"\" for property: " +
							getAttribute().getName()
					);
				}
			}
		}
		return isInsertable;
	}
	public boolean isUpdateable() {
		PropertyGeneration generation = getPropertyGeneration();
		boolean isUpdateable = DomHelper.extractBooleanAttributeValue( getElement(), "update", true );
		if ( isUpdateable && generation == PropertyGeneration.ALWAYS ) {
			final org.dom4j.Attribute updateAttribute = getElement().attribute( "update" );
			if ( updateAttribute == null ) {
				// updateable only because the user did not specify
				// anything; just override it
				isUpdateable = false;
			}
			else {
				// the user specifically supplied update="true",
				// which constitutes an illegal combo
				throw new MappingException(
						"cannot specify both update=\"true\" and generated=\"" + generation.getName() +
						"\" for property: " +
						getAttribute().getName()
				);
			}
		}
		return isUpdateable;
	}
	public boolean isKeyCasadeDeleteEnabled() {
		//TODO: implement
		return false;
	}
	public String getUnsavedValue() {
		//TODO: implement
		return null;
	}
}
