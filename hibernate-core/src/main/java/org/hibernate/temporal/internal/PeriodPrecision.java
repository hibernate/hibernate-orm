package org.hibernate.temporal.internal;

import java.time.Instant;

import org.hibernate.MappingException;
import org.hibernate.TemporalValueException;
import org.hibernate.audit.spi.ChangelogSupplier;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.temporal.spi.TemporalPeriodPrecisionContext;
import org.hibernate.dialect.temporal.spi.TemporalPeriodPrecisionContext.Source;
import org.hibernate.dialect.temporal.spi.TemporalPeriodPrecisionSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.AuxiliaryTableHolder;
import org.hibernate.mapping.BasicValue;
import org.hibernate.temporal.TemporalTableStrategy;

/// The immutable, factory-owned storage contract shared by Instant period endpoints.
/// Resolution uses mapped column information only, never a JDBC connection. Ordinary
/// attributes, audit keys and version attributes do not participate in this contract.
///
/// @author Steve Ebersole
public final class PeriodPrecision {
	private final TemporalPeriodPrecisionSupport support;
	private final String role;

	private PeriodPrecision(TemporalPeriodPrecisionSupport support, String role) {
		this.support = support;
		this.role = role;
	}

	public static PeriodPrecision resolve(MetadataImplementor metadata, SessionFactoryImplementor factory) {
		if ( !factory.getSessionFactoryOptions().isTemporalPreservationEnabled() ) {
			return null;
		}
		PeriodPrecision resolved = null;
		for ( var entity : metadata.getEntityBindings() ) {
			resolved = resolve( metadata, factory, entity, entity.getEntityName(), resolved );
		}
		for ( var collection : metadata.getCollectionBindings() ) {
			resolved = resolve( metadata, factory, collection, collection.getRole(), resolved );
		}
		return resolved;
	}

	private static PeriodPrecision resolve(
			MetadataImplementor metadata, SessionFactoryImplementor factory,
			AuxiliaryTableHolder holder, String role, PeriodPrecision resolved) {
		final var dialect = factory.getJdbcServices().getDialect();
		final var strategy = factory.getSessionFactoryOptions().getTemporalTableStrategy();
		final var coordinator = factory.getChangesetCoordinator();
		final var source = strategy == TemporalTableStrategy.NATIVE ? Source.NATIVE
				: coordinator.useServerTimestamp( dialect ) ? Source.INLINE
				: coordinator.getIdentifierSupplier() instanceof CurrentTimestampChangesetIdentifierSupplier ? Source.FETCHED
				: coordinator.getIdentifierSupplier() instanceof ChangelogSupplier<?> ? Source.CHANGELOG
				: Source.JAVA;
		for ( String endpoint : new String[] { "rowStart", "rowEnd" } ) {
			final var column = holder.getAuxiliaryColumn( endpoint );
			if ( column == null || !(column.getValue() instanceof BasicValue value) ) {
				continue;
			}
			final var type = value.resolve();
			if ( type.getRelationalJavaType().getJavaTypeClass() != Instant.class ) {
				continue;
			}
			final var context = new TemporalPeriodPrecisionContext(
					strategy, type.getJdbcType(), column.getSqlType( metadata ),
					dialect.getTemporalTableSupport().getTemporalColumnPrecision(), source );
			final var support = dialect.getTemporalTableSupport().getTemporalPeriodPrecisionSupport( context );
			final var location = role + "." + endpoint;
			if ( support == null || !support.isSupported() ) {
				throw new MappingException( "Unsupported temporal period precision for " + location
						+ " [" + dialect.getClass().getSimpleName() + ", " + context + "]: "
						+ (support == null ? "missing contribution" : support.getReason())
						+ "; contribute a compatible period storage/producer contract or use temporal_preservation=false for legacy interpretation" );
			}
			if ( source == Source.INLINE
					&& (support.getCurrentExpression() == null || support.getCurrentExpression().isBlank()) ) {
				throw new MappingException( "Missing transaction-stable fixed-point period producer for " + location
						+ " [" + context + "]" );
			}
			if ( resolved == null ) {
				resolved = new PeriodPrecision( support, location );
			}
			else if ( !resolved.support.getSignature().equals( support.getSignature() ) ) {
				throw new MappingException( "Incompatible temporal period precision contracts: " + resolved.role
						+ " [" + resolved.support.getSignature() + "] and " + location + " [" + support.getSignature() + "]" );
			}
		}
		return resolved;
	}

	/// Prepare a Hibernate-owned timestamp once before publishing it in the session.
	/// Application-owned changelog identities must already be representable and are
	/// never replaced. Non-Instant identifiers retain their existing identity semantics.
	public Object prepare(Object value, boolean applicationOwned) {
		if ( !(value instanceof Instant instant) ) {
			return value;
		}
		final var canonical = support.canonicalize( instant );
		if ( applicationOwned && !canonical.equals( instant ) ) {
			throw new TemporalValueException( "Application-owned @Changelog Instant identifier " + instant
					+ " is not representable by temporal period contract " + support.getSignature()
					+ " for " + role + "; supply a representable identifier. Hibernate will not rekey the changelog" );
		}
		return canonical;
	}

	public String getCurrentExpression() {
		return support.getCurrentExpression();
	}

	public String getSignature() {
		return support.getSignature();
	}
}
