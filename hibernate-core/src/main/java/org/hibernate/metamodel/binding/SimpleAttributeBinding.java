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

import org.dom4j.Attribute;
import org.dom4j.Element;

import org.hibernate.MappingException;
import org.hibernate.mapping.PropertyGeneration;
import org.hibernate.metamodel.source.hbm.HbmHelper;
import org.hibernate.metamodel.source.util.DomHelper;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class SimpleAttributeBinding extends SingularAttributeBinding {
	private PropertyGeneration generation;
	private boolean isLazy;

	SimpleAttributeBinding(EntityBinding entityBinding) {
		super( entityBinding );
	}

	public void fromHbmXml(MappingDefaults defaults, Element element, org.hibernate.metamodel.domain.Attribute attribute) {
		super.fromHbmXml( defaults, element, attribute );
		this.isLazy = DomHelper.extractBooleanAttributeValue( element, "lazy", false );
		this.generation = PropertyGeneration.parse( DomHelper.extractAttributeValue( element, "generated", null ) );
        if ( generation == PropertyGeneration.ALWAYS || generation == PropertyGeneration.INSERT ) {
	        // generated properties can *never* be insertable...
	        if ( isInsertable() ) {
				final Attribute insertAttribute = element.attribute( "insert" );
		        if ( insertAttribute == null ) {
			        // insertable simply because the user did not specify anything; just override it
					setInsertable( false );
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

	        // properties generated on update can never be updateable...
	        if ( isUpdateable() && generation == PropertyGeneration.ALWAYS ) {
				final Attribute updateAttribute = element.attribute( "update" );
		        if ( updateAttribute == null ) {
			        // updateable only because the user did not specify
			        // anything; just override it
			        setUpdateable( false );
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
        }
	}

	protected boolean isLazyDefault(MappingDefaults defaults) {
		return false;
	}

	@Override
	public boolean isSimpleValue() {
		return true;
	}

	public PropertyGeneration getGeneration() {
		return generation;
	}

	@Override
	public boolean isLazy() {
		return isLazy;
	}

	@Override
	public boolean isEmbedded() {
		return false;
	}
}
