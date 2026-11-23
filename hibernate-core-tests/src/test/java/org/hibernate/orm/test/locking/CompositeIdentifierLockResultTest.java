package org.hibernate.orm.test.locking;

import java.math.BigDecimal;
import java.util.BitSet;
import java.util.List;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.creation.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.from.TableReference;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.exec.internal.lock.CompositeIdentifierLockResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.BigDecimalJavaType;
import org.hibernate.type.descriptor.java.IntegerJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/// Exercises relational identity matching independently of database row order,
/// identifier equality, converters, and mutable extraction buffers.
///
/// @author Steve Ebersole
class CompositeIdentifierLockResultTest {
	@Test
	void matchesCompleteKeysInAnyOrderAndKeepsOriginalIdentifiers() {
		final var f = new Fixture( IntegerJavaType.INSTANCE, IntegerJavaType.INSTANCE );
		final var first = new Identifier( "first", new Object[] { 1, 2 } );
		final var second = new Identifier( "second", new Object[] { 1, 3 } );
		final var result = f.result( first, second, first );
		final var assembler = result.createResultAssembler( null, null );
		assertThat( assembler.assemble( f.row( 1, 3 ) ) ).isSameAs( second );
		assertThat( assembler.assemble( f.row( 1, 2 ) ) ).isSameAs( first );
		assertThat( assembler.getInitializer() ).isNull();
		assertThat( assembler.getAssembledJavaType().getJavaTypeClass() ).isEqualTo( Identifier.class );
		final var cacheIndexes = new BitSet();
		result.collectValueIndexesToCache( cacheIndexes );
		assertThat( cacheIndexes.stream().toArray() ).containsExactly( 1, 3 );
		verifyNoInteractions( f.session );
	}

	@Test
	void handlesSingleColumnCompositeMappings() {
		final var f = new Fixture( IntegerJavaType.INSTANCE );
		final var id = new Identifier( "one", new Object[] { 4 } );
		assertThat( f.result( id ).createResultAssembler( null, null ).assemble( f.row( 4 ) ) ).isSameAs( id );
	}

	@Test
	void coalescesHibernateEqualIdentifiersWithoutDependingOnJavaEquality() {
		final var f = new Fixture( IntegerJavaType.INSTANCE );
		when( f.identifierType.getTypeForEqualsHashCode() ).thenReturn( f.identifierType );
		when( f.identifierType.getHashCode( any(), any() ) ).thenReturn( 1 );
		when( f.identifierType.isEqual( any(), any(), any() ) ).thenAnswer( call ->
				call.getArgument( 0, Identifier.class ).name.equals( call.getArgument( 1, Identifier.class ).name ) );
		final var first = new Identifier( "same", new Object[] { 1 } );
		final var equivalent = new Identifier( "same", new Object[] { 1 } );
		assertThat( first ).isNotEqualTo( equivalent );
		assertThat( f.result( first, equivalent ).createResultAssembler( null, null ).assemble( f.row( 1 ) ) ).isSameAs( first );
		verify( f.identifier, times( 1 ) ).breakDownJdbcValues( any(), any( ModelPart.JdbcValueConsumer.class ), same( f.session ) );
	}

	@Test
	void separateExecutionsKeepTheirOwnCanonicalIdentifiers() {
		final var f = new Fixture( IntegerJavaType.INSTANCE );
		final var first = new Identifier( "execution one", new Object[] { 1 } );
		final var second = new Identifier( "execution two", new Object[] { 1 } );
		final var firstResult = f.result( first );
		final var secondResult = f.result( second );
		assertThat( secondResult.createResultAssembler( null, null ).assemble( f.row( 1 ) ) ).isSameAs( second );
		assertThat( firstResult.createResultAssembler( null, null ).assemble( f.row( 1 ) ) ).isSameAs( first );
	}

	@Test
	void usesJdbcTypeEqualityAndSnapshotsMutableComponents() {
		final var f = new Fixture( BigDecimalJavaType.INSTANCE, PrimitiveByteArrayJavaType.INSTANCE );
		final var bytes = new byte[] { 1, 2 };
		final var id = new Identifier( "typed", new Object[] { new BigDecimal( "1.00" ), bytes } );
		final var result = f.result( id );
		bytes[0] = 9;
		assertThat( result.createResultAssembler( null, null ).assemble( f.row( new BigDecimal( "1.0" ), new byte[] { 1, 2 } ) ) )
				.isSameAs( id );
	}

	@Test
	void failsForAnUnexpectedTupleRatherThanMatchingItsFirstComponent() {
		final var f = new Fixture( IntegerJavaType.INSTANCE, IntegerJavaType.INSTANCE );
		final var result = f.result( new Identifier( "first", new Object[] { 1, 2 } ) );
		assertThatThrownBy( () -> result.createResultAssembler( null, null ).assemble( f.row( 1, 3 ) ) )
				.isInstanceOf( AssertionFailure.class ).hasMessageContaining( "unexpected_table" ).hasMessageContaining( "TestEntity" );
	}

	@Test
	void rejectsDistinctIdentitiesWithTheSameRelationalKey() {
		final var f = new Fixture( IntegerJavaType.INSTANCE );
		assertThatThrownBy( () -> f.result( new Identifier( "one", new Object[] { 1 } ), new Identifier( "two", new Object[] { 1 } ) ) )
				.isInstanceOf( AssertionFailure.class ).hasMessageContaining( "Distinct expected identities" )
				.hasMessageContaining( "unexpected_table" ).hasMessageContaining( "TestEntity" );
	}

	private record Identifier(String name, Object[] values) {}

	private static class Fixture {
		final CompositeIdentifierMapping identifier = mock( CompositeIdentifierMapping.class );
		final TableDetails table = mock( TableDetails.class );
		final TableDetails.KeyDetails keys = mock( TableDetails.KeyDetails.class );
		final EntityPersister persister = mock( EntityPersister.class );
		final Type identifierType = mock( Type.class );
		final SharedSessionContractImplementor session = mock( SharedSessionContractImplementor.class );
		final TableReference tableReference = mock( TableReference.class );
		final DomainResultCreationState creationState = mock( DomainResultCreationState.class );
		final int columns;

		Fixture(JavaType<?>... types) {
			columns = types.length;
			final var entity = mock( EntityMappingType.class );
			when( identifier.findContainingEntityMapping() ).thenReturn( entity );
			when( entity.getEntityName() ).thenReturn( "TestEntity" );
			final var embeddable = mock( EmbeddableMappingType.class );
			when( identifier.getMappedIdEmbeddableTypeDescriptor() ).thenReturn( embeddable );
			when( persister.getRootEntityName() ).thenReturn( "TestEntity" );
			when( persister.getIdentifierType() ).thenReturn( identifierType );
			when( table.getTableName() ).thenReturn( "unexpected_table" );
			when( table.getKeyDetails() ).thenReturn( keys );
			when( keys.getColumnCount() ).thenReturn( columns );
			final var sqlState = mock( SqlAstCreationState.class );
			final var sqlContext = mock( SqlAstCreationContext.class );
			final var resolver = mock( SqlExpressionResolver.class );
			final var typeConfiguration = new TypeConfiguration();
			when( embeddable.getJavaType() ).thenReturn( (JavaType) typeConfiguration.getJavaTypeRegistry().resolveDescriptor( Identifier.class ) );
			when( creationState.getSqlAstCreationState() ).thenReturn( sqlState );
			when( sqlState.getCreationContext() ).thenReturn( sqlContext );
			when( sqlContext.getTypeConfiguration() ).thenReturn( typeConfiguration );
			when( sqlState.getSqlExpressionResolver() ).thenReturn( resolver );
			for ( int i = 0; i < columns; i++ ) {
				final var column = mock( TableDetails.KeyColumn.class );
				final var jdbcMapping = mock( JdbcMapping.class );
				when( jdbcMapping.getJdbcJavaType() ).thenReturn( (JavaType) types[i] );
				when( column.getJdbcMapping() ).thenReturn( jdbcMapping );
				when( keys.getKeyColumn( i ) ).thenReturn( column );
				final var expression = mock( Expression.class );
				final var selection = mock( SqlSelection.class );
				when( selection.getValuesArrayPosition() ).thenReturn( 2 * i + 1 );
				when( resolver.resolveSqlExpression( tableReference, column ) ).thenReturn( expression );
				when( resolver.resolveSqlSelection( same( expression ), any(), isNull(), same( typeConfiguration ) ) ).thenReturn( selection );
			}
			doAnswer( call -> {
				final var id = call.getArgument( 0, Identifier.class );
				final var consumer = call.getArgument( 1, ModelPart.JdbcValueConsumer.class );
				for ( int i = 0; i < columns; i++ ) {
					consumer.consume( i, id.values[i], keys.getKeyColumn( i ) );
				}
				return columns;
			} ).when( identifier ).breakDownJdbcValues( any(), any( ModelPart.JdbcValueConsumer.class ), same( session ) );
		}

		CompositeIdentifierLockResult result(Identifier... ids) {
			return new CompositeIdentifierLockResult( table, identifier, tableReference, null, creationState,
					List.of( ids ).stream().map( id -> new EntityKey( id, persister ) ).toList(), session );
		}

		RowProcessingState row(Object... values) {
			final var row = mock( RowProcessingState.class );
			for ( int i = 0; i < values.length; i++ ) {
				when( row.getJdbcValue( 2 * i + 1 ) ).thenReturn( values[i] );
			}
			return row;
		}
	}
}
