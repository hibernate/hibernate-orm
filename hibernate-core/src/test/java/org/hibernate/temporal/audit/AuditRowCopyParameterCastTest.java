package org.hibernate.temporal.audit;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.Audited;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.sql.ast.internal.H2SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.dialect.temporal.internal.DefaultTemporalTableSupport;
import org.hibernate.dialect.temporal.spi.TemporalTableSupport;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.cfg.StateManagementSettings.AUDIT_STRATEGY;
import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// The changeset identifier of a copied audit row is a parameter of a select
/// list, which some dialects must cast. The cast is to the type of the column,
/// so that the copied row and the row it invalidates hold the same timestamp,
/// even when the column is more precise than a timestamp is by default.
@JiraKey("HHH-21285")
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = {
		@Setting(name = DIALECT, value = "org.hibernate.temporal.audit.AuditRowCopyParameterCastTest$CastingDialect"),
		@Setting(name = AUDIT_STRATEGY, value = "VALIDITY"),
		@Setting(name = CHANGESET_ID_SUPPLIER,
				value = "org.hibernate.temporal.audit.AuditRowCopyParameterCastTest$NanosecondChangesets")
})
@DomainModel(annotatedClasses = AuditRowCopyParameterCastTest.Thing.class)
@SessionFactory
class AuditRowCopyParameterCastTest {

	@Audited
	@Entity(name = "CastThing")
	@Table(name = "cast_thing")
	static class Thing {
		@Id int id = 1;
		String name;
	}

	/// Casts a parameter of a select list, and declares the temporal
	/// columns with nanosecond precision, as DB2 does.
	public static class CastingDialect extends H2Dialect {
		@Override
		public TemporalTableSupport getTemporalTableSupport() {
			return new DefaultTemporalTableSupport( this ) {
				@Override
				public int getTemporalColumnPrecision() {
					return 9;
				}
			};
		}

		@Override
		public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
			return new StandardSqlAstTranslatorFactory() {
				@Override
				protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
						SqlAstTranslationRequest<S, T> request) {
					return new H2SqlAstTranslator<>( request ) {
						@Override
						protected void renderSelectExpression(Expression expression) {
							renderSelectExpressionWithCastedOrInlinedPlainParameters( expression );
						}
					};
				}
			};
		}
	}

	/// Timestamps which lose their last digits when cast to the default precision.
	public static class NanosecondChangesets implements ChangesetIdentifierSupplier<Instant> {
		private Instant last = Instant.parse( "2026-01-01T00:00:00.000000111Z" );

		@Override
		public Instant generateIdentifier(SharedSessionContract session) {
			last = last.plusSeconds( 1 );
			return last;
		}
	}

	@Test
	void copiedRowHoldsTheTimestampWhichInvalidatesThePreviousRow(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var thing = new Thing();
			thing.name = "original";
			session.persist( thing );
		} );
		scope.inTransaction( session -> session.find( Thing.class, 1 ).name = "updated" );

		try ( var auditLog = AuditLogFactory.create( scope.getSessionFactory() ) ) {
			final List<Object> changesets = auditLog.getChangesets( Thing.class, 1 );
			assertEquals( 2, changesets.size() );
			assertEquals( 111, ( (Instant) changesets.get( 1 ) ).getNano() );
			assertEquals( "original", findAt( scope, changesets.get( 0 ) ).name );
			assertEquals( "updated", findAt( scope, changesets.get( 1 ) ).name );
		}
	}

	private static Thing findAt(SessionFactoryScope scope, Object changeset) {
		try ( var session = scope.getSessionFactory().withOptions().atChangeset( changeset ).open() ) {
			return session.find( Thing.class, 1 );
		}
	}
}
