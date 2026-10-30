package org.hibernate.sql.results.jdbc.internal;

import java.util.List;
import java.util.Set;

import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingResolution;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

/// Keeps initializer lock modes aligned with a non-null-target plan's sole root
/// alias. The provider's result mapping and assembler resolution remain delegated.
/// Only immutable plan metadata is retained, with no session or execution state.
///
/// @author Steve Ebersole
@Internal
public final class NonNullTargetJdbcValuesMappingProducer implements JdbcValuesMappingProducer {
	private final JdbcValuesMappingProducer delegate;
	private final String lockedAlias;

	public NonNullTargetJdbcValuesMappingProducer(JdbcValuesMappingProducer delegate, String lockedAlias) {
		this.delegate = delegate;
		this.lockedAlias = lockedAlias;
	}

	@Override
	public JdbcValuesMapping resolve(JdbcValuesMetadata metadata, LoadQueryInfluencers influencers, SessionFactoryImplementor factory) {
		return new TargetedMapping( delegate.resolve( metadata, influencers, factory ), lockedAlias );
	}

	@Override
	public void addAffectedTableNames(Set<String> names, SessionFactoryImplementor factory) {
		delegate.addAffectedTableNames( names, factory );
	}

	/// Execution-local view of the provider's resolved mapping. Initializer plans
	/// are resolved by the delegate so cached assembler behavior is preserved.
	public static final class TargetedMapping implements JdbcValuesMapping {
		private final JdbcValuesMapping delegate;
		private final String lockedAlias;

		private TargetedMapping(JdbcValuesMapping delegate, String lockedAlias) {
			this.delegate = delegate;
			this.lockedAlias = lockedAlias;
		}

		public LockMode determineTargetLockMode(String alias, LockMode requested) {
			return lockedAlias.equals( alias ) ? requested : LockMode.NONE;
		}

		@Override public List<SqlSelection> getSqlSelections() { return delegate.getSqlSelections(); }
		@Override public int getRowSize() { return delegate.getRowSize(); }
		@Override public int[] getValueIndexesToCacheIndexes() { return delegate.getValueIndexesToCacheIndexes(); }
		@Override public int getRowToCacheSize() { return delegate.getRowToCacheSize(); }
		@Override public List<DomainResult<?>> getDomainResults() { return delegate.getDomainResults(); }
		@Override public JdbcValuesMappingResolution resolveAssemblers(SessionFactoryImplementor factory) { return delegate.resolveAssemblers( factory ); }
		@Override public LockMode determineDefaultLockMode(String alias, LockMode defaultMode) { return delegate.determineDefaultLockMode( alias, defaultMode ); }
	}
}
