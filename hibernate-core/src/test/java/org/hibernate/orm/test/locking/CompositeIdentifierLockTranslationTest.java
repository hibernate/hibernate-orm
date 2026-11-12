package org.hibernate.orm.test.locking;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.LockMode;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.internal.lock.TableLock;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.JdbcSettings.DIALECT;

/// Exercises the acquisition AST's existing tuple-predicate emulation without
/// executing SQL or changing production dialect capability profiles.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = CompositeIdentifierFollowOnLockingTest.EmbeddedItem.class)
@SessionFactory(exportSchema = false)
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = @Setting(name = DIALECT,
		value = "org.hibernate.orm.test.locking.CompositeIdentifierLockTranslationTest$NoTuplesDialect"))
class CompositeIdentifierLockTranslationTest {
	@Test
	void emulatesCompositeAcquisitionPredicates(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var persister = session.getFactory().getMappingMetamodel()
					.getEntityDescriptor( CompositeIdentifierFollowOnLockingTest.EmbeddedItem.class );
			final var table = new AtomicReference<TableDetails>();
			persister.forEachTableDetails( details -> { if ( details.isIdentifierTable() ) { table.set( details ); } } );
			final var lock = new ExposedTableLock( table.get(), persister, List.of(
					session.generateEntityKey( new CompositeIdentifierFollowOnLockingTest.Pair( 1, 2 ), persister ),
					session.generateEntityKey( new CompositeIdentifierFollowOnLockingTest.Pair( 1, 3 ), persister ) ), session );
			assertThat( lock.sql( session ) ).contains( "local_key", "tenant_key", " or " ).doesNotContain( " in (" );
		} );
	}

	private static class ExposedTableLock extends TableLock {
		ExposedTableLock(TableDetails table, EntityMappingType mapping, List<EntityKey> keys, SessionImplementor session) {
			super( table, mapping, keys, session );
		}

		String sql(SessionImplementor session) {
			final var options = new QueryOptionsImpl();
			options.getLockOptions().setLockMode( LockMode.PESSIMISTIC_WRITE );
			return session.getDialect().getSqlAstTranslatorFactory().buildTranslator(
					new SqlAstTranslationRequest.Select( session.getFactory(), new SelectStatement( querySpec, domainResults ) ) )
					.translate( jdbcParameterBindings, options ).getSqlString();
		}
	}

	public static class NoTuplesDialect extends H2Dialect {
		@Override
		public RowValueSupport getRowValueSupport() {
			return RowValueSupport.NONE;
		}
	}
}
