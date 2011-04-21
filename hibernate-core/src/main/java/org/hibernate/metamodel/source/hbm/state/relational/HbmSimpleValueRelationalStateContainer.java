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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.MappingException;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.metamodel.binding.MappingDefaults;
import org.hibernate.metamodel.binding.SimpleAttributeBinding;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLColumnElement;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLDiscriminator;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLId;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLProperty;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLTimestamp;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLVersion;

/**
 * @author Gail Badner
 */
public class HbmSimpleValueRelationalStateContainer implements SimpleAttributeBinding.TupleRelationalState {
	private final MappingDefaults defaults;
	private final Set<String> propertyUniqueKeys;
	private final Set<String> propertyIndexes;
	private final Set<SimpleAttributeBinding.SingleValueRelationalState> singleValueStates;

	public NamingStrategy getNamingStrategy() {
		return defaults.getNamingStrategy();
	}

	// TODO: remove duplication after Id, Discriminator, Version, Timestamp, and Property extend a common interface.

	public HbmSimpleValueRelationalStateContainer(MappingDefaults defaults,
												  boolean autoColumnCreation,
												  XMLId id) {
		this( defaults, id.getColumnElement() );
		if ( singleValueStates.isEmpty() ) {
			if ( id.getColumn() == null && ! autoColumnCreation ) {
				throw new MappingException( "No columns to map and auto column creation is disabled." );
			}
			singleValueStates.add( new HbmColumnRelationalState( id, this ) );
		}
		else if ( id.getColumn() != null ) {
			throw new MappingException( "column attribute may not be used together with <column> subelement" );
		}
	}

	public HbmSimpleValueRelationalStateContainer(MappingDefaults defaults,
												  boolean autoColumnCreation,
												  XMLDiscriminator discriminator) {
		this( defaults, discriminator.getFormulaElement(), discriminator.getColumnElement() );
		if ( singleValueStates.isEmpty() ) {
			if ( discriminator.getColumn() == null && discriminator.getFormula() == null &&  ! autoColumnCreation ) {
				throw new MappingException( "No column or formula to map and auto column creation is disabled." );
			}
			singleValueStates.add( new HbmColumnRelationalState( discriminator, this ) );
		}
		else if ( discriminator.getColumn() != null || discriminator.getFormula() != null) {
			throw new MappingException( "column/formula attribute may not be used together with <column>/<formula> subelement" );
		}
	}

	public HbmSimpleValueRelationalStateContainer(MappingDefaults defaults,
												  boolean autoColumnCreation,
												  XMLVersion version) {
		this( defaults, version.getColumnElement() );
		if ( singleValueStates.isEmpty() ) {
			if ( version.getColumn() == null && ! autoColumnCreation ) {
				throw new MappingException( "No column or formula to map and auto column creation is disabled." );
			}
			singleValueStates.add( new HbmColumnRelationalState( version, this ) );
		}
		else if ( version.getColumn() != null ) {
			throw new MappingException( "column attribute may not be used together with <column> subelement" );
		}
	}

	public HbmSimpleValueRelationalStateContainer(MappingDefaults defaults,
												  boolean autoColumnCreation,
												  XMLTimestamp timestamp) {
		this( defaults, null );
		if ( singleValueStates.isEmpty() ) {
			if ( timestamp.getColumn() == null && ! autoColumnCreation ) {
				throw new MappingException( "No columns to map and auto column creation is disabled." );
			}
			singleValueStates.add( new HbmColumnRelationalState( timestamp, this ) );
		}
		else if ( timestamp.getColumn() != null ) {
			throw new MappingException( "column attribute may not be used together with <column> subelement" );
		}
	}

	public HbmSimpleValueRelationalStateContainer(MappingDefaults defaults,
												  boolean autoColumnCreation,
												  XMLProperty property) {
		this( defaults, property.getColumnElementOrFormulaElement() );
		if ( singleValueStates.isEmpty() ) {
			if ( property.getColumn() == null && property.getFormula() == null &&  ! autoColumnCreation ) {
				throw new MappingException( "No column or formula to map and auto column creation is disabled." );
			}
			singleValueStates.add( new HbmColumnRelationalState( property, this ) );
		}
		else if ( property.getColumn() != null || property.getFormula() != null) {
			throw new MappingException( "column/formula attribute may not be used together with <column>/<formula> subelement" );
		}
	}

	private HbmSimpleValueRelationalStateContainer(MappingDefaults defaults,
												   String formulaElement,
												   XMLColumnElement columnElement
	) {
		this( defaults,
				formulaElement != null ?
						Collections.singletonList( formulaElement ) :
						columnElement != null ? Collections.singletonList( columnElement ) : Collections.<Object>emptyList()
		);
	}

	private HbmSimpleValueRelationalStateContainer(MappingDefaults defaults,
												  List mappedColumnsOrFormulas) {
		this.defaults = defaults;
		this.propertyUniqueKeys = Collections.emptySet();
		this.propertyIndexes = Collections.emptySet();
		singleValueStates = new LinkedHashSet<SimpleAttributeBinding.SingleValueRelationalState>(
							mappedColumnsOrFormulas == null || mappedColumnsOrFormulas.isEmpty() ?
									1 :
									mappedColumnsOrFormulas.size()
		);
		if ( mappedColumnsOrFormulas != null && ! mappedColumnsOrFormulas.isEmpty() ) {
			for ( Object mappedColumnOrFormula : mappedColumnsOrFormulas ) {
				singleValueStates.add( createColumnOrFormulaRelationalState( this, mappedColumnOrFormula ) );
			}
		}
	}

	private static SimpleAttributeBinding.SingleValueRelationalState createColumnOrFormulaRelationalState(
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

	public Set<SimpleAttributeBinding.SingleValueRelationalState> getSingleValueRelationalStates() {
		return singleValueStates;
	}

	Set<String> getPropertyUniqueKeys() {
		return propertyUniqueKeys;
	}

	Set<String> getPropertyIndexes() {
		return propertyIndexes;
	}
}
