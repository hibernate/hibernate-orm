/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import org.hibernate.testing.util.MappingTableHelper;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.QualifiedNameImpl;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.pipeline.internal.MappingResolutionOptions;
import org.hibernate.boot.spi.GlobalMappingDefaults;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.id.enhanced.DatabaseStructure;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.enhanced.SequenceStructure;
import org.hibernate.id.enhanced.TableStructure;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.mapping.Table;
import org.hibernate.relational.naming.spi.QualifiedPhysicalName;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.orm.test.namingstrategy.PhysicalNamespaceRestorationTest.environment;
import static org.hibernate.orm.test.namingstrategy.PhysicalNamespaceRestorationTest.registry;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/// Generator archives retain finalized names without retaining a comparison policy.
///
/// @author Steve Ebersole
class GeneratorPhysicalNameRestorationTest {
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void restoresGeneratorAndArchivedCommands(boolean sequence) {
		final var sourceEnvironment = environment( "source:", false );
		final var options = mock( MappingResolutionOptions.class );
		final var registry = registry( sourceEnvironment );
		when( options.getServiceRegistry() ).thenReturn( registry );
		when( options.getMappingDefaults() ).thenReturn( mock( GlobalMappingDefaults.class ) );
		when( options.getPhysicalNamingStrategy() ).thenReturn( PhysicalNamingStrategyStandardImpl.INSTANCE );
		final var database = new Database( options );
		final var name = new QualifiedNameImpl( Identifier.toIdentifier( "catalog" ),
				Identifier.toIdentifier( "`Schema`" ), Identifier.toIdentifier( "`Generator`" ) );
		final DatabaseStructure structure;
		if ( sequence ) {
			structure = new SequenceStructure( "orm", name, 1, 1, Long.class );
		}
		else {
			// Reuse a contributed table, avoiding unrelated type resolution in this archive test.
			final var namespace = database.locateNamespace( database.toLogicalName( "catalog" ),
					database.toLogicalName( "`Schema`" ) );
			namespace.createTable( database.toLogicalName( "`Generator`" ),
					physical -> new org.hibernate.mapping.PhysicalTable( "orm", namespace, physical, false ) );
			structure = new TableStructure( "orm", name, Identifier.toIdentifier( "next_value" ), 1, 1, Long.class );
		}
		structure.registerExportables( database );
		final var sourceName = structure.getPhysicalName();
		final var targetFactory = environment( "restored:", true ).getIdentifierHelper().getPhysicalNameFactory();
		final var expected = new QualifiedPhysicalName( targetFactory.create( "catalog", false ),
				targetFactory.create( "Schema", true ), targetFactory.create( "Generator", true ) );
		final var context = mock( SqlStringGenerationContext.class );
		when( context.getDialect() ).thenReturn( new H2Dialect() );
		when( context.getPhysicalNameFactory() ).thenReturn( targetFactory );
		when( context.format( any( QualifiedPhysicalName.class ) ) ).thenAnswer( invocation -> {
			assertEquals( expected, invocation.getArgument( 0 ), "Use the target policy and retain finalized quoting" );
			return "catalog.\"Schema\".\"Generator\"";
		} );

		final var restored = (DatabaseStructure) SerializationHelper.clone( (java.io.Serializable) structure );
		assertThrows( IllegalStateException.class, restored::getPhysicalName );
		restored.initialize( context );
		assertEquals( expected, restored.getPhysicalName() );
		assertNotEquals( sourceName, restored.getPhysicalName() );
		final var restoredAgain = (DatabaseStructure) SerializationHelper.clone( (java.io.Serializable) restored );
		restoredAgain.initialize( context );
		assertEquals( expected, restoredAgain.getPhysicalName() );

		// Schema commands can execute without initializing the captured generator first.
		final var owner = MappingTableHelper.table( "orm", "owner", targetFactory );
		structure.registerExtraExportables( owner, mock( Optimizer.class ) );
		final var restoredOwner = (Table) SerializationHelper.clone( owner );
		final var commands = ((org.hibernate.mapping.PhysicalTable) restoredOwner).getResetCommands( context );
		assertEquals( 1, commands.size() );
		assertTrue( commands.get( 0 ).initCommands()[0].contains( "catalog.\"Schema\".\"Generator\"" ) );
	}
}
