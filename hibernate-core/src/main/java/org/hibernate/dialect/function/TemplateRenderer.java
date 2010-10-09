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
package org.hibernate.dialect.function;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.engine.SessionFactoryImplementor;

/**
 * Delegate for handling function "templates". 
 *
 * @author Steve Ebersole
 */
public class TemplateRenderer {
	private static final Logger log = LoggerFactory.getLogger( TemplateRenderer.class );

	private final String template;
	private final String[] chunks;
	private final int[] paramIndexes;

	@SuppressWarnings({ "UnnecessaryUnboxing" })
	public TemplateRenderer(String template) {
		this.template = template;

		List<String> chunkList = new ArrayList<String>();
		List<Integer> paramList = new ArrayList<Integer>();
		StringBuffer chunk = new StringBuffer( 10 );
		StringBuffer index = new StringBuffer( 2 );

		for ( int i = 0; i < template.length(); ++i ) {
			char c = template.charAt( i );
			if ( c == '?' ) {
				chunkList.add( chunk.toString() );
				chunk.delete( 0, chunk.length() );

				while ( ++i < template.length() ) {
					c = template.charAt( i );
					if ( Character.isDigit( c ) ) {
						index.append( c );
					}
					else {
						chunk.append( c );
						break;
					}
				}

				paramList.add( Integer.valueOf( index.toString() ) );
				index.delete( 0, index.length() );
			}
			else {
				chunk.append( c );
			}
		}

		if ( chunk.length() > 0 ) {
			chunkList.add( chunk.toString() );
		}

		chunks = chunkList.toArray( new String[chunkList.size()] );
		paramIndexes = new int[paramList.size()];
		for ( int i = 0; i < paramIndexes.length; ++i ) {
			paramIndexes[i] = paramList.get( i ).intValue();
		}
	}

	public String getTemplate() {
		return template;
	}

	public int getAnticipatedNumberOfArguments() {
		return paramIndexes.length;
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	public String render(List args, SessionFactoryImplementor factory) {
		int numberOfArguments = args.size();
		if ( getAnticipatedNumberOfArguments() > 0 && numberOfArguments != getAnticipatedNumberOfArguments() ) {
			log.warn( "Function template anticipated {} arguments, but {} arguments encountered",
					getAnticipatedNumberOfArguments(), numberOfArguments );
		}
		StringBuffer buf = new StringBuffer();
		for ( int i = 0; i < chunks.length; ++i ) {
			if ( i < paramIndexes.length ) {
				final int index = paramIndexes[i] - 1;
				final Object arg =  index < numberOfArguments ? args.get( index ) : null;
				if ( arg != null ) {
					buf.append( chunks[i] ).append( arg );
				}
			}
			else {
				buf.append( chunks[i] );
			}
		}
		return buf.toString();
	}
}
