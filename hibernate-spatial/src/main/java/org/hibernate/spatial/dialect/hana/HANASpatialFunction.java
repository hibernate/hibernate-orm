/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.hana;

import java.util.BitSet;
import java.util.List;

import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

public class HANASpatialFunction extends StandardSQLFunction {

	private static final String AS_EWKB_SUFFIX = ".ST_AsEWKB()";

	private final BitSet argumentIsGeometryTypeMask = new BitSet();
	private final boolean staticFunction;

	public HANASpatialFunction(String name, boolean firstArgumentIsGeometryType) {
		super( name );
		this.argumentIsGeometryTypeMask.set( 1, firstArgumentIsGeometryType );
		this.staticFunction = false;
	}

	public HANASpatialFunction(String name, boolean firstArgumentIsGeometryType, boolean staticFunction) {
		super( name );
		this.argumentIsGeometryTypeMask.set( staticFunction ? 0 : 1, firstArgumentIsGeometryType );
		this.staticFunction = staticFunction;
	}

	public HANASpatialFunction(String name, Type registeredType, boolean firstArgumentIsGeometryType) {
		super( name, registeredType );
		this.argumentIsGeometryTypeMask.set( 1, firstArgumentIsGeometryType );
		this.staticFunction = false;
	}

	public HANASpatialFunction(String name, Type registeredType, boolean[] argumentIsGeometryTypeMask) {
		super( name, registeredType );
		for ( int i = 0; i < argumentIsGeometryTypeMask.length; i++ ) {
			this.argumentIsGeometryTypeMask.set( i + 1, argumentIsGeometryTypeMask[i] );
		}
		this.staticFunction = false;
	}

	public HANASpatialFunction(String name, Type registeredType, boolean firstArgumentIsGeometryType, boolean staticFunction) {
		super( name, registeredType );
		this.argumentIsGeometryTypeMask.set( staticFunction ? 0 : 1, firstArgumentIsGeometryType );
		this.staticFunction = staticFunction;
	}

	@Override
	public String render(Type firstArgumentType, List arguments, SessionFactoryImplementor sessionFactory) {
		if ( arguments.size() == 0 ) {
			return getName() + "()";
		}

		final StringBuilder buf = new StringBuilder();
		int firstArgumentIndex;
		if ( this.staticFunction ) {
			// Add function call
			buf.append( getName() );
			firstArgumentIndex = 0;
		}
		else {
			// If the first argument is an expression, e.g. a nested function, strip the .ST_AsEWKB() suffix
			Object argument = arguments.get( 0 );
			final boolean parseFromWKB = ( "?".equals( argument ) );
			appendArgument( argument, parseFromWKB, buf );
			// Add function call
			buf.append( "." ).append( getName() );

			firstArgumentIndex = 1;
		}

		buf.append( '(' );

		// Add function arguments
		for ( int i = firstArgumentIndex; i < arguments.size(); i++ ) {
			final Object argument = arguments.get( i );
			// Check if first argument needs to be parsed from EWKB. This is the case if the first argument is a
			// parameter that is set as EWKB or if it's a nested function call.
			final boolean parseFromWKB = ( isGeometryArgument( i ) && "?".equals( argument ) );
			appendArgument( argument, parseFromWKB, buf );
			if ( i < arguments.size() - 1 ) {
				buf.append( ", " );
			}
		}
		buf.append( ')' );
		// If it doesn't specify an explicit type, assume it's a geometry
		if ( this.getType() == null ) {
			buf.append( AS_EWKB_SUFFIX );
		}
		return buf.toString();
	}

	private Object stripEWKBSuffix(Object argument) {
		if ( ( argument instanceof String ) && ( (String) argument ).endsWith( AS_EWKB_SUFFIX ) ) {
			String argumentString = (String) argument;
			return argumentString.substring( 0, argumentString.length() - AS_EWKB_SUFFIX.length() );
		}

		return argument;
	}

	private boolean isGeometryArgument(int idx) {
		return this.argumentIsGeometryTypeMask.size() > idx && this.argumentIsGeometryTypeMask.get( idx );
	}

	private void appendArgument(Object argument, boolean parseFromWKB, StringBuilder buf) {
		if ( parseFromWKB ) {
			buf.append( "ST_GeomFromEWKB(" );
		}
		buf.append( stripEWKBSuffix( argument ) );
		if ( parseFromWKB ) {
			buf.append( ")" );
		}
	}
}
