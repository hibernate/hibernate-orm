/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.Parameter;

import org.hibernate.QueryParameterException;
import org.hibernate.engine.query.spi.NamedParameterDescriptor;
import org.hibernate.engine.query.spi.OrdinalParameterDescriptor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;
import org.hibernate.type.Type;

/**
 * Encapsulates metadata about parameters encountered within a query.
 *
 * @author Steve Ebersole
 */
public class ParameterMetadataImpl implements ParameterMetadata {
	private static final OrdinalParameterDescriptor[] EMPTY_ORDINALS = new OrdinalParameterDescriptor[0];

	private final OrdinalParameterDescriptor[] ordinalDescriptors;
	private final Map<String,NamedParameterDescriptor> namedDescriptorMap;
	private boolean isOrdinalParametersZeroBased = true;

	private ParameterMetadataImpl(
			OrdinalParameterDescriptor[] ordinalDescriptors,
			Map<String, NamedParameterDescriptor> namedDescriptorMap, boolean isOrdinalParametersZeroBased) {
		this.ordinalDescriptors = ordinalDescriptors;
		this.namedDescriptorMap = namedDescriptorMap;
		this.isOrdinalParametersZeroBased = isOrdinalParametersZeroBased;
	}

	/**
	 * Instantiates a ParameterMetadata container.
	 *
	 * @param ordinalDescriptors Descriptors of the ordinal parameters
	 * @param namedDescriptorMap Descriptors of the named parameters
	 */
	public ParameterMetadataImpl(
			OrdinalParameterDescriptor[] ordinalDescriptors,
			Map<String,NamedParameterDescriptor> namedDescriptorMap) {
		if ( ordinalDescriptors == null ) {
			this.ordinalDescriptors = EMPTY_ORDINALS;
		}
		else {
			final OrdinalParameterDescriptor[] copy = new OrdinalParameterDescriptor[ ordinalDescriptors.length ];
			System.arraycopy( ordinalDescriptors, 0, copy, 0, ordinalDescriptors.length );
			this.ordinalDescriptors = copy;
		}

		if ( namedDescriptorMap == null ) {
			this.namedDescriptorMap = java.util.Collections.emptyMap();
		}
		else {
			final int size = (int) ( ( namedDescriptorMap.size() / .75 ) + 1 );
			final Map<String,NamedParameterDescriptor> copy = new HashMap<>( size );
			copy.putAll( namedDescriptorMap );
			this.namedDescriptorMap = java.util.Collections.unmodifiableMap( copy );
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<QueryParameter<?>> collectAllParameters() {
		if ( hasNamedParameters() || hasPositionalParameters() ) {
			final HashSet allParameters = new HashSet();
			allParameters.addAll( namedDescriptorMap.values() );
			allParameters.addAll( ArrayHelper.toList( ordinalDescriptors ) );
			return allParameters;
		}

		return Collections.emptySet();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<Parameter<?>> collectAllParametersJpa() {
		if ( hasNamedParameters() || hasPositionalParameters() ) {
			final HashSet allParameters = new HashSet();
			allParameters.addAll( namedDescriptorMap.values() );
			allParameters.addAll( ArrayHelper.toList( ordinalDescriptors ) );
			return allParameters;
		}

		return Collections.emptySet();
	}

	@Override
	public boolean hasNamedParameters() {
		return !namedDescriptorMap.isEmpty();
	}

	@Override
	public boolean hasPositionalParameters() {
		return getOrdinalParameterCount() > 0;
	}

	@Override
	public int getPositionalParameterCount() {
		return getOrdinalParameterCount();
	}

	public int getOrdinalParameterCount() {
		return ordinalDescriptors.length;
	}

	/**
	 * Get the descriptor for an ordinal parameter given its position
	 *
	 * @param position The position (0 based)
	 *
	 * @return The ordinal parameter descriptor
	 *
	 * @throws QueryParameterException If the position is out of range
	 */
	public OrdinalParameterDescriptor getOrdinalParameterDescriptor(int position) {
		if ( !isOrdinalParametersZeroBased ) {
			position--;
		}
		if ( position < 0 || position >= ordinalDescriptors.length ) {
			throw new QueryParameterException(
					"Position beyond number of declared ordinal parameters. " +
							"Remember that ordinal parameters are 0-based! Position: " + position
			);
		}
		return ordinalDescriptors[position];
	}

	/**
	 * Deprecated.
	 *
	 * @param position The position
	 *
	 * @return The type
	 *
	 * @deprecated Use {@link OrdinalParameterDescriptor#getExpectedType()} from the
	 * {@link #getOrdinalParameterDescriptor} return instead
	 */
	@Deprecated
	public Type getOrdinalParameterExpectedType(int position) {
		return getOrdinalParameterDescriptor( position ).getExpectedType();
	}

	/**
	 * Deprecated.
	 *
	 * @param position The position
	 *
	 * @return The source location
	 *
	 * @deprecated Use {@link OrdinalParameterDescriptor#getSourceLocation()} from the
	 * {@link #getOrdinalParameterDescriptor} return instead
	 */
	@Deprecated
	public int getOrdinalParameterSourceLocation(int position) {
		return getOrdinalParameterDescriptor( position ).getSourceLocation();
	}

	/**
	 * Access to the names of all named parameters
	 *
	 * @return The named parameter names
	 */
	public Set<String> getNamedParameterNames() {
		return  namedDescriptorMap.keySet();
	}

	@Override
	public <T> QueryParameter<T> getQueryParameter(String name) {
		return getNamedParameterDescriptor( name );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> QueryParameter<T> getQueryParameter(Integer position) {
		return getOrdinalParameterDescriptor( position );
	}

	@Override
	public <T> QueryParameter<T> resolve(Parameter<T> param) {
		if ( param instanceof QueryParameter ) {
			return (QueryParameter<T>) param;
		}

		if ( param.getName() != null ) {
			return getQueryParameter( param.getName() );
		}

		if ( param.getPosition() != null ) {
			return getQueryParameter( param.getPosition() );
		}

		throw new IllegalArgumentException( "Could not resolve javax.persistence.Parameter to org.hibernate.query.QueryParameter" );
	}

	/**
	 * Get the descriptor for a named parameter given the name
	 *
	 * @param name The name of the parameter to locate
	 *
	 * @return The named parameter descriptor
	 *
	 * @throws QueryParameterException If the name could not be resolved to a named parameter
	 */
	public NamedParameterDescriptor getNamedParameterDescriptor(String name) {
		final NamedParameterDescriptor meta = namedDescriptorMap.get( name );
		if ( meta == null ) {
			throw new QueryParameterException( "could not locate named parameter [" + name + "]" );
		}
		return meta;
	}

	/**
	 * Deprecated.
	 *
	 * @param name The name of the parameter
	 *
	 * @return The type
	 *
	 * @deprecated Use {@link NamedParameterDescriptor#getExpectedType()} from the
	 * {@link #getNamedParameterDescriptor} return instead
	 */
	@Deprecated
	public Type getNamedParameterExpectedType(String name) {
		return getNamedParameterDescriptor( name ).getExpectedType();
	}

	/**
	 * Deprecated.
	 *
	 * @param name The name of the parameter
	 *
	 * @return The type
	 *
	 * @deprecated Use {@link NamedParameterDescriptor#getSourceLocations()} from the
	 * {@link #getNamedParameterDescriptor} return instead
	 */
	@Deprecated
	public int[] getNamedParameterSourceLocations(String name) {
		return getNamedParameterDescriptor( name ).getSourceLocations();
	}

	@Override
	public boolean isOrdinalParametersZeroBased() {
		return isOrdinalParametersZeroBased;
	}

	@Override
	public void setOrdinalParametersZeroBased(boolean isZeroBased) {
		this.isOrdinalParametersZeroBased = isZeroBased;
	}

	public ParameterMetadataImpl getOrdinalParametersZeroBasedCopy() {
		return new ParameterMetadataImpl(
				this.ordinalDescriptors,
				this.namedDescriptorMap,
				true
		);
	}
}
