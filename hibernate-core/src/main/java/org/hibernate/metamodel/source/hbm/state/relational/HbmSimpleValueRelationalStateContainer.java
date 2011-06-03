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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.metamodel.binding.HibernateTypeDescriptor;
import org.hibernate.metamodel.relational.state.SimpleValueRelationalState;
import org.hibernate.metamodel.relational.state.TupleRelationalState;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLColumnElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping.XMLClass.XMLDiscriminator;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping.XMLClass.XMLId;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping.XMLClass.XMLTimestamp;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping.XMLClass.XMLVersion;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLManyToOneElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLPropertyElement;
import org.hibernate.metamodel.source.spi.BindingContext;

/**
 * @author Gail Badner
 */
public class HbmSimpleValueRelationalStateContainer implements TupleRelationalState {
	private final BindingContext bindingContext;
	private final Set<String> propertyUniqueKeys;
	private final Set<String> propertyIndexes;
	private final List<SimpleValueRelationalState> simpleValueStates;
	private final HibernateTypeDescriptor hibernateTypeDescriptor = new HibernateTypeDescriptor();

	public BindingContext getBindingContext() {
		return bindingContext;
	}

	public NamingStrategy getNamingStrategy() {
		return getBindingContext().getNamingStrategy();
	}

	// TODO: remove duplication after Id, Discriminator, Version, Timestamp, and Property extend a common interface.

	public HbmSimpleValueRelationalStateContainer(
			BindingContext bindingContext,
			boolean autoColumnCreation,
			XMLId id) {
		this( bindingContext, id.getColumn() );
		if ( simpleValueStates.isEmpty() ) {
			if ( id.getColumn() == null && ! autoColumnCreation ) {
				throw new MappingException( "No columns to map and auto column creation is disabled." );
			}
			simpleValueStates.add( new HbmColumnRelationalState( id, this ) );
		}
		else if ( id.getColumn() != null ) {
			throw new MappingException( "column attribute may not be used together with <column> subelement" );
		}
		this.hibernateTypeDescriptor.setTypeName( id.getTypeAttribute() );
	}

	public HbmSimpleValueRelationalStateContainer(
			BindingContext bindingContext,
			boolean autoColumnCreation,
			XMLDiscriminator discriminator) {
		this( bindingContext, discriminator.getFormula(), discriminator.getColumn() );
		if ( simpleValueStates.isEmpty() ) {
			if ( discriminator.getColumn() == null && discriminator.getFormula() == null &&  ! autoColumnCreation ) {
				throw new MappingException( "No column or formula to map and auto column creation is disabled." );
			}
			simpleValueStates.add( new HbmColumnRelationalState( discriminator, this ) );
		}
		else if ( discriminator.getColumn() != null || discriminator.getFormula() != null) {
			throw new MappingException( "column/formula attribute may not be used together with <column>/<formula> subelement" );
		}
		this.hibernateTypeDescriptor.setTypeName( discriminator.getType() == null ? "string" : discriminator.getType() );
	}

	public HbmSimpleValueRelationalStateContainer(
			BindingContext bindingContext,
			boolean autoColumnCreation,
			XMLVersion version) {
		this( bindingContext, version.getColumn() );
		if ( simpleValueStates.isEmpty() ) {
			if ( version.getColumn() == null && ! autoColumnCreation ) {
				throw new MappingException( "No column or formula to map and auto column creation is disabled." );
			}
			simpleValueStates.add( new HbmColumnRelationalState( version, this ) );
		}
		else if ( version.getColumn() != null ) {
			throw new MappingException( "column attribute may not be used together with <column> subelement" );
		}
		this.hibernateTypeDescriptor.setTypeName( version.getType() == null ? "integer" : version.getType() );
	}

	public HbmSimpleValueRelationalStateContainer(
			BindingContext bindingContext,
			boolean autoColumnCreation,
			XMLTimestamp timestamp) {
		this( bindingContext, null );
		if ( simpleValueStates.isEmpty() ) {
			if ( timestamp.getColumn() == null && ! autoColumnCreation ) {
				throw new MappingException( "No columns to map and auto column creation is disabled." );
			}
			simpleValueStates.add( new HbmColumnRelationalState( timestamp, this ) );
		}
		else if ( timestamp.getColumn() != null ) {
			throw new MappingException( "column attribute may not be used together with <column> subelement" );
		}
		this.hibernateTypeDescriptor.setTypeName( "db".equals( timestamp.getSource() ) ? "dbtimestamp" : "timestamp" );
	}

	public HbmSimpleValueRelationalStateContainer(
			BindingContext bindingContext,
			boolean autoColumnCreation,
			XMLPropertyElement property) {
		this( bindingContext, property.getColumnOrFormula() );
		if ( simpleValueStates.isEmpty() ) {
			if ( property.getColumn() == null && property.getFormula() == null &&  ! autoColumnCreation ) {
				throw new MappingException( "No column or formula to map and auto column creation is disabled." );
			}
			simpleValueStates.add( new HbmColumnRelationalState( property, this ) );
		}
		else if ( property.getColumn() != null || property.getFormula() != null) {
			throw new MappingException( "column/formula attribute may not be used together with <column>/<formula> subelement" );
		}
		this.hibernateTypeDescriptor.setTypeName( property.getTypeAttribute() );
	}

	public HbmSimpleValueRelationalStateContainer(
			BindingContext bindingContext,
			boolean autoColumnCreation,
			XMLManyToOneElement manyToOne) {
		this( bindingContext, manyToOne.getColumnOrFormula() );
		if ( simpleValueStates.isEmpty() ) {
			if ( manyToOne.getColumn() == null && manyToOne.getFormula() == null &&  ! autoColumnCreation ) {
				throw new MappingException( "No column or formula to map and auto column creation is disabled." );
			}
			simpleValueStates.add( new HbmColumnRelationalState( manyToOne, this ) );
		}
		else if ( manyToOne.getColumn() != null || manyToOne.getFormula() != null) {
			throw new MappingException( "column/formula attribute may not be used together with <column>/<formula> subelement" );
		}
	}

	private HbmSimpleValueRelationalStateContainer(
			BindingContext bindingContext,
			String formulaElement,
			XMLColumnElement columnElement) {
		this( bindingContext,
				formulaElement != null
						? Collections.singletonList( formulaElement )
						: columnElement != null
								? Collections.singletonList( columnElement )
								: Collections.<Object>emptyList()
		);
	}

	private HbmSimpleValueRelationalStateContainer(
			BindingContext bindingContext,
			List mappedColumnsOrFormulas) {
		this.bindingContext = bindingContext;
		this.propertyUniqueKeys = Collections.emptySet();
		this.propertyIndexes = Collections.emptySet();
		simpleValueStates = new ArrayList<SimpleValueRelationalState>(
							mappedColumnsOrFormulas == null || mappedColumnsOrFormulas.isEmpty()
									? 1
									: mappedColumnsOrFormulas.size()
		);
		if ( mappedColumnsOrFormulas != null && ! mappedColumnsOrFormulas.isEmpty() ) {
			for ( Object mappedColumnOrFormula : mappedColumnsOrFormulas ) {
				simpleValueStates.add( createColumnOrFormulaRelationalState( this, mappedColumnOrFormula ) );
			}
		}
	}

	private static SimpleValueRelationalState createColumnOrFormulaRelationalState(
			HbmSimpleValueRelationalStateContainer container,
			Object columnOrFormula) {
		if ( XMLColumnElement.class.isInstance( columnOrFormula ) ) {
			return new HbmColumnRelationalState(
					XMLColumnElement.class.cast( columnOrFormula ),
					container
			);
		}
		else if ( String.class.isInstance( columnOrFormula ) ) {
			return new HbmDerivedValueRelationalState( String.class.cast( columnOrFormula ) );
		}
		throw new MappingException( "unknown type of column or formula: " + columnOrFormula.getClass().getName() );
	}

	public List<SimpleValueRelationalState> getRelationalStates() {
		return simpleValueStates;
	}

	Set<String> getPropertyUniqueKeys() {
		return propertyUniqueKeys;
	}

	Set<String> getPropertyIndexes() {
		return propertyIndexes;
	}
}
