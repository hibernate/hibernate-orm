/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.metamodel.internal.source.hbm.parser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;

import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryScalarReturn;
import org.hibernate.engine.spi.NamedQueryDefinitionBuilder;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinitionBuilder;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.jaxb.spi.Origin;
import org.hibernate.jaxb.spi.hbm.JaxbLoadCollectionElement;
import org.hibernate.jaxb.spi.hbm.JaxbReturnElement;
import org.hibernate.jaxb.spi.hbm.JaxbReturnJoinElement;
import org.hibernate.jaxb.spi.hbm.JaxbReturnScalarElement;
import org.hibernate.jaxb.spi.hbm.JaxbSynchronizeElement;
import org.hibernate.metamodel.internal.source.hbm.BindHelper;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.source.LocalBindingContext;
import org.hibernate.type.Type;

/**
 * @author Brett Meyer
 */
public class SQLQueryElementContentParser extends AbstractQueryElementContentsParser {
	private List<String> synchronizedTables = new ArrayList<String>();
	private List<JaxbLoadCollectionElement> loadCollectionElements = new ArrayList<JaxbLoadCollectionElement>();
	private List<JaxbReturnScalarElement> returnScalarElements = new ArrayList<JaxbReturnScalarElement>();
	private List<JaxbReturnElement> returnElements = new ArrayList<JaxbReturnElement>();
	private List<JaxbReturnJoinElement> returnJoinElements = new ArrayList<JaxbReturnJoinElement>();

	@Override
	protected void parseExtra( String queryName, Serializable obj,
			NamedQueryDefinitionBuilder builder ) {
		if ( !JAXBElement.class.isInstance( obj ) ) {
			return;
		}
		NamedSQLQueryDefinitionBuilder sqlBuilder
				= NamedSQLQueryDefinitionBuilder.class.cast( builder );
		JAXBElement jaxbElement = JAXBElement.class.cast( obj );
		Class targetType = jaxbElement.getDeclaredType();
		Object value = jaxbElement.getValue();
		if ( JaxbSynchronizeElement.class == targetType ) {
			JaxbSynchronizeElement element = JaxbSynchronizeElement.class.cast(
					value );
			synchronizedTables.add( element.getTable() );
		}
		else if ( JaxbLoadCollectionElement.class == targetType ) {
			loadCollectionElements.add( JaxbLoadCollectionElement.class.cast(
					value ) );
		}
		else if ( JaxbReturnScalarElement.class == targetType ) {
			returnScalarElements.add( JaxbReturnScalarElement.class.cast(
					value ) );
		}
		else if ( JaxbReturnElement.class == targetType ) {
			returnElements.add( JaxbReturnElement.class.cast( value ) );
		}
		else if ( JaxbReturnJoinElement.class == targetType ) {
			returnJoinElements.add( JaxbReturnJoinElement.class.cast( value ) );
		}
	}

	public NamedSQLQueryDefinition buildQueryReturns( String name,
			NamedSQLQueryDefinitionBuilder builder, 
			Origin origin, LocalBindingContext bindingContext,
			MetadataImplementor metadata ) {
		final ResultSetMappingDefinition definition
				= new ResultSetMappingDefinition( name );
		int cnt = 0;
		for ( final JaxbReturnScalarElement r : returnScalarElements ) {
			String column = r.getColumn();
			String typeFromXML = r.getType();
			Type type = StringHelper.isNotEmpty( typeFromXML )
					? metadata.getTypeResolver().heuristicType( typeFromXML )
							: null;
			definition.addQueryReturn( new NativeSQLQueryScalarReturn(
					column, type ) );
		}
		for ( final JaxbReturnJoinElement r : returnJoinElements ) {
			definition.addQueryReturn( 
					BindHelper.bindReturnJoin( r, cnt++, origin ) );

		}
		for ( final JaxbLoadCollectionElement r : loadCollectionElements ) {
			definition.addQueryReturn( 
					BindHelper.bindLoadCollection(
							r, cnt++, origin, bindingContext ) );

		}
		for ( final JaxbReturnElement r : returnElements ) {
			definition.addQueryReturn( BindHelper.bindReturn(
					r, cnt++, origin, metadata, bindingContext ) );

		}
		
		return builder.setQueryReturns( definition.getQueryReturns() )
				.setQuerySpaces( synchronizedTables )
				.createNamedQueryDefinition();
	}
}
