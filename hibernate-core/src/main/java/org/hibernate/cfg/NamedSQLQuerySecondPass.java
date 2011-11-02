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
package org.hibernate.cfg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.jboss.logging.Logger;

import org.hibernate.MappingException;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;

/**
 * @author Emmanuel Bernard
 */
public class NamedSQLQuerySecondPass extends ResultSetMappingBinder implements QuerySecondPass {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class,
                                                                       NamedSQLQuerySecondPass.class.getName());

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
		Integer timeout = tAtt == null ? null : Integer.valueOf( tAtt.getValue() );
		Attribute fsAtt = queryElem.attribute( "fetch-size" );
		Integer fetchSize = fsAtt == null ? null : Integer.valueOf( fsAtt.getValue() );
		Attribute roAttr = queryElem.attribute( "read-only" );
		boolean readOnly = roAttr != null && "true".equals( roAttr.getValue() );
		Attribute cacheModeAtt = queryElem.attribute( "cache-mode" );
		String cacheMode = cacheModeAtt == null ? null : cacheModeAtt.getValue();
		Attribute cmAtt = queryElem.attribute( "comment" );
		String comment = cmAtt == null ? null : cmAtt.getValue();

		java.util.List<String> synchronizedTables = new ArrayList<String>();
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
					queryName,
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
					queryName,
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

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "Named SQL query: %s -> %s", namedQuery.getName(), namedQuery.getQueryString() );
		}
		mappings.addSQLQuery( queryName, namedQuery );
	}
}
