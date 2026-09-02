/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.TableInsertStandard;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.spi.mutation.TableMapping.MutationDetails;
import org.hibernate.sql.spi.mutation.jdbc.JdbcInsertMutation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Verifies every community Dialect no-column-insert rendering profile.
///
/// @author Steve Ebersole
public class NoColumnInsertRenderingTests {
	@Test
	void communityTranslatorsPreserveDefaultValuesSyntax() {
		for ( Dialect dialect : List.of(
				new SQLServerLegacyDialect(),
				new PostgreSQLLegacyDialect(),
				new CockroachLegacyDialect(),
				new GaussDBDialect(),
				new FirebirdDialect(),
				new SQLiteDialect(),
				new CacheDialect(),
				new InterSystemsIRISDialect() ) ) {
			assertThat( translate( dialect ).getSqlString() )
					.as( dialect.getClass().getSimpleName() )
					.isEqualTo( "insert into example_table default values" );
		}
	}

	@Test
	void communityTranslatorsPreserveSpecializedValuesSyntax() {
		assertThat( translate( new InformixDialect() ).getSqlString() )
				.isEqualTo( "insert into example_table values (0)" );
		assertThat( translate( new SybaseAnywhereDialect() ).getSqlString() )
				.isEqualTo( "insert into example_table values (default)" );
	}

	@Test
	void hanaLegacyFailsDuringTranslation() {
		assertThatThrownBy( () -> translate( new HANALegacyDialect() ) )
				.isInstanceOf( MappingException.class )
				.hasMessageContaining( "example_table" )
				.hasMessageContaining( "contains no column" );
	}

	private static JdbcInsertMutation translate(Dialect dialect) {
		final SessionFactoryImplementor sessionFactory = mock( SessionFactoryImplementor.class );
		final SessionFactoryOptions sessionFactoryOptions = mock( SessionFactoryOptions.class );
		final JdbcServices jdbcServices = mock( JdbcServices.class );
		when( sessionFactory.getJdbcServices() ).thenReturn( jdbcServices );
		when( sessionFactory.getSessionFactoryOptions() ).thenReturn( sessionFactoryOptions );
		when( jdbcServices.getDialect() ).thenReturn( dialect );

		final TableMapping tableMapping = mock( TableMapping.class );
		when( tableMapping.getTableName() ).thenReturn( "example_table" );
		when( tableMapping.getInsertDetails() ).thenReturn( new MutationDetails(
				MutationType.INSERT,
				new Expectation.RowCount(),
				null,
				false
		) );

		final MutationTarget mutationTarget = mock( MutationTarget.class );
		when( mutationTarget.getNavigableRole() ).thenReturn( new NavigableRole( "Example" ) );
		when( mutationTarget.getIdentifierTableMapping() ).thenReturn( tableMapping );

		final TableInsertStandard insert = new TableInsertStandard(
				new MutatingTableReference( tableMapping ),
				mutationTarget,
				List.of(),
				List.of(),
				List.of()
		);
		final SqlAstTranslatorFactory factory = dialect.getSqlAstTranslatorFactory();
		return factory.buildTranslator(
				new SqlAstTranslationRequest.ModelMutation<JdbcInsertMutation>( sessionFactory, insert )
		).translate( null, null );
	}
}
