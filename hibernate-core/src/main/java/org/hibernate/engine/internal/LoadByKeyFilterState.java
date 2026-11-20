package org.hibernate.engine.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.internal.FilterImpl;

import jakarta.annotation.Nullable;

/** A snapshot of the filters which may change the entity selected by a unique key. */
final class LoadByKeyFilterState {
	static final LoadByKeyFilterState UNKNOWN = new LoadByKeyFilterState( null );

	private final @Nullable Map<String, List<FilterParameterSnapshot>> filters;

	private LoadByKeyFilterState(@Nullable Map<String, List<FilterParameterSnapshot>> filters) {
		this.filters = filters;
	}

	static LoadByKeyFilterState capture(LoadQueryInfluencers influencers) {
		final Map<String, List<FilterParameterSnapshot>> filters = new HashMap<>();
		for ( String name : influencers.getEnabledFilterNames() ) {
			final var filter = influencers.getEnabledFilter( name );
			final var definition = ((FilterImpl) filter).getFilterDefinition();
			if ( definition.isAppliedToLoadByKey() ) {
				final List<FilterParameterSnapshot> parameters = new ArrayList<>();
				for ( String parameter : definition.getParameterNames() ) {
					parameters.add( FilterParameterSnapshot.capture( name, parameter,
							definition.getParameterJdbcMapping( parameter ).getJavaTypeDescriptor(),
							filter.getParameterValue( parameter ) ) );
				}
				filters.put( name, parameters );
			}
		}
		return new LoadByKeyFilterState( filters );
	}

	boolean matches(LoadQueryInfluencers influencers) {
		if ( filters == null ) {
			return false;
		}
		int count = 0;
		for ( String name : influencers.getEnabledFilterNames() ) {
			final var filter = influencers.getEnabledFilter( name );
			if ( filter.getFilterDefinition().isAppliedToLoadByKey() ) {
				count++;
				final var parameters = filters.get( name );
				if ( parameters == null ) {
					return false;
				}
				for ( var parameter : parameters ) {
					if ( !parameter.matches( filter.getParameterValue( parameter.parameterName() ) ) ) {
						return false;
					}
				}
			}
		}
		return count == filters.size();
	}
}
