/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.cfg;

import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.util.StringHelper;
import org.hibernate.engine.NamedSQLQueryDefinition;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Emmanuel Bernard
 */
public class NamedSQLQuerySecondPass extends ResultSetMappingBinder implements QuerySecondPass {
	private static Logger log = LoggerFactory.getLogger( NamedSQLQuerySecondPass.class);
	private Element queryElem;
	private String path;
	private Mappings mappings;

	public NamedSQLQuerySecondPass(Element queryElem, String path, Mappings mappings) {
		this.queryElem = queryElem;
		this.path = path;
		this.mappings = mappings;
	}

	public void doSecondPass(Map persistentClasses) throws MappingException {
		String queryName = queryElem.attribute( "name" ).getValue();
		if (path!=null) queryName = path + '.' + queryName;

		boolean cacheable = "true".equals( queryElem.attributeValue( "cacheable" ) );
		String region = queryElem.attributeValue( "cache-region" );
		Attribute tAtt = queryElem.attribute( "timeout" );
		Integer timeout = tAtt == null ? null : new Integer( tAtt.getValue() );
		Attribute fsAtt = queryElem.attribute( "fetch-size" );
		Integer fetchSize = fsAtt == null ? null : new Integer( fsAtt.getValue() );
		Attribute roAttr = queryElem.attribute( "read-only" );
		boolean readOnly = roAttr != null && "true".equals( roAttr.getValue() );
		Attribute cacheModeAtt = queryElem.attribute( "cache-mode" );
		String cacheMode = cacheModeAtt == null ? null : cacheModeAtt.getValue();
		Attribute cmAtt = queryElem.attribute( "comment" );
		String comment = cmAtt == null ? null : cmAtt.getValue();

		java.util.List synchronizedTables = new ArrayList();
		Iterator tables = queryElem.elementIterator( "synchronize" );
		while ( tables.hasNext() ) {
			synchronizedTables.add( ( (Element) tables.next() ).attributeValue( "table" ) );
		}
		boolean callable = "true".equals( queryElem.attributeValue( "callable" ) );

		NamedSQLQueryDefinition namedQuery;
		Attribute ref = queryElem.attribute( "resultset-ref" );
		String resultSetRef = ref == null ? null : ref.getValue();
		if ( StringHelper.isNotEmpty( resultSetRef ) ) {
			namedQuery = new NamedSQLQueryDefinition(
					queryElem.getText(),
					resultSetRef,
					synchronizedTables,
					cacheable,
					region,
					timeout,
					fetchSize,
					HbmBinder.getFlushMode( queryElem.attributeValue( "flush-mode" ) ),
					HbmBinder.getCacheMode( cacheMode ),
					readOnly,
					comment,
					HbmBinder.getParameterTypes( queryElem ),
					callable
			);
			//TODO check there is no actual definition elemnents when a ref is defined
		}
		else {
			ResultSetMappingDefinition definition = buildResultSetMappingDefinition( queryElem, path, mappings );
			namedQuery = new NamedSQLQueryDefinition(
					queryElem.getText(),
					definition.getQueryReturns(),
					synchronizedTables,
					cacheable,
					region,
					timeout,
					fetchSize,
					HbmBinder.getFlushMode( queryElem.attributeValue( "flush-mode" ) ),
					HbmBinder.getCacheMode( cacheMode ),
					readOnly,
					comment,
					HbmBinder.getParameterTypes( queryElem ),
					callable
			);
		}

		log.debug( "Named SQL query: " + queryName + " -> " + namedQuery.getQueryString() );
		mappings.addSQLQuery( queryName, namedQuery );
	}
}
