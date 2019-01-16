/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.persistence.Parameter;

import org.hibernate.QueryException;
import org.hibernate.QueryParameterException;
import org.hibernate.engine.query.spi.NamedParameterDescriptor;
import org.hibernate.engine.query.spi.OrdinalParameterDescriptor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.compare.ComparableComparator;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;
import org.hibernate.type.Type;

/**
 * Encapsulates metadata about parameters encountered within a query.
 *
 * @author Steve Ebersole
 */
public class ParameterMetadataImpl implements ParameterMetadata {

	private final Map<Integer,OrdinalParameterDescriptor> ordinalDescriptorMap;
	private final Map<String,NamedParameterDescriptor> namedDescriptorMap;

	//Important: queries with large amounts of parameters need the following
	//cache to have efficient performance on #containsReference(QueryParameter).
	private final Set<QueryParameter> ordinalDescriptorValueCache;
	private final Set<QueryParameter> namedDescriptorValueCache;

	public ParameterMetadataImpl(
			Map<Integer,OrdinalParameterDescriptor> ordinalDescriptorMap,
			Map<String, NamedParameterDescriptor> namedDescriptorMap) {
		this.ordinalDescriptorMap = ordinalDescriptorMap == null
				? Collections.emptyMap()
				: Collections.unmodifiableMap( ordinalDescriptorMap );
		this.ordinalDescriptorValueCache = this.ordinalDescriptorMap.isEmpty()
				? Collections.emptySet()
				: Collections.unmodifiableSet( new HashSet<>( this.ordinalDescriptorMap.values() ) );
		this.namedDescriptorMap = namedDescriptorMap == null
				? Collections.emptyMap()
				: Collections.unmodifiableMap( namedDescriptorMap );
		this.namedDescriptorValueCache = this.namedDescriptorMap.isEmpty()
				? Collections.emptySet()
				: Collections.unmodifiableSet( new HashSet<>( this.namedDescriptorMap.values() ) );

		if (ordinalDescriptorMap != null &&  ! ordinalDescriptorMap.isEmpty() ) {
			final List<Integer> sortedPositions = new ArrayList<>( ordinalDescriptorMap.keySet() );
			sortedPositions.sort( ComparableComparator.INSTANCE );

			int lastPosition = -1;
			for ( Integer sortedPosition : sortedPositions ) {
				if ( lastPosition == -1 ) {
					lastPosition = sortedPosition;
					continue;
				}

				if ( sortedPosition != lastPosition + 1 ) {
					throw new QueryException(
							String.format(
									Locale.ROOT,
									"Unexpected gap in ordinal parameter labels [%s -> %s] : [%s]",
									lastPosition,
									sortedPosition,
									StringHelper.join( ",", sortedPositions.iterator() )
							)
					);
				}

				lastPosition = sortedPosition;
			}

		}
	}

	@Override
	public Collection<QueryParameter> getPositionalParameters() {
		return ordinalDescriptorValueCache;
	}

	@Override
	public Collection<QueryParameter> getNamedParameters() {
		return namedDescriptorValueCache;
	}

	@Override
	public int getParameterCount() {
		return ordinalDescriptorMap.size() + namedDescriptorMap.size();
	}

	@Override
	@SuppressWarnings("SuspiciousMethodCalls")
	public boolean containsReference(QueryParameter parameter) {
		return ordinalDescriptorValueCache.contains( parameter )
				|| namedDescriptorValueCache.contains( parameter );
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
		return ordinalDescriptorMap.size();
	}

	@Override
	public Set<String> getNamedParameterNames() {
		return  namedDescriptorMap.keySet();
	}

	public Set<Integer> getOrdinalParameterLabels() {
		return ordinalDescriptorMap.keySet();
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
		final OrdinalParameterDescriptor descriptor = ordinalDescriptorMap.get( position );
		if ( descriptor == null ) {
			throw new IllegalArgumentException(
					String.format(
							Locale.ROOT,
							"Could not locate ordinal parameter [%s], expecting one of [%s]",
							position,
							StringHelper.join( ", ", ordinalDescriptorMap.keySet().iterator())
					)
			);
		}
		return descriptor;
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
	 * @deprecated Use {@link OrdinalParameterDescriptor#getPosition()} from the
	 * {@link #getOrdinalParameterDescriptor} return instead
	 */
	@Deprecated
	public int getOrdinalParameterSourceLocation(int position) {
		return getOrdinalParameterDescriptor( position ).getPosition();
	}

	@Override
	public <T> QueryParameter<T> getQueryParameter(String name) {
		//noinspection unchecked
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
		final NamedParameterDescriptor descriptor = namedDescriptorMap.get( name );
		if ( descriptor == null ) {
			throw new IllegalArgumentException(
					String.format(
							Locale.ROOT,
							"Could not locate named parameter [%s], expecting one of [%s]",
							name,
							String.join( ", ", namedDescriptorMap.keySet() )
					)
			);
		}
		return descriptor;
	}

	@Override
	public void visitRegistrations(Consumer<QueryParameter> action) {
		if ( hasPositionalParameters() ) {
			for ( OrdinalParameterDescriptor descriptor : ordinalDescriptorMap.values() ) {
				action.accept( descriptor );
			}
		}
		else if ( hasNamedParameters() ) {
			for ( NamedParameterDescriptor descriptor : namedDescriptorMap.values() ) {
				action.accept( descriptor );
			}
		}
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
	 * @deprecated Use {@link NamedParameterDescriptor#getPosition()} from the
	 * {@link #getNamedParameterDescriptor} return instead
	 */
	@Deprecated
	public int[] getNamedParameterSourceLocations(String name) {
		return getNamedParameterDescriptor( name ).getSourceLocations();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<QueryParameter<?>> collectAllParameters() {
		if ( hasNamedParameters() || hasPositionalParameters() ) {
			final HashSet allParameters = new HashSet();
			allParameters.addAll( namedDescriptorMap.values() );
			allParameters.addAll( ordinalDescriptorMap.values() );
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
			allParameters.addAll( ordinalDescriptorMap.values() );
			return allParameters;
		}

		return Collections.emptySet();
	}
}
