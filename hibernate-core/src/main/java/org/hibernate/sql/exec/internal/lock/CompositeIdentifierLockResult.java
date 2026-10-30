package org.hibernate.sql.exec.internal.lock;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.TableDetails;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.from.TableReference;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.type.descriptor.java.JavaType;

/// Identifies acquired rows by their relational key values, returning the original
/// identifier of an expected entity instead of reconstructing its identifier graph.
/// Each instance belongs to a single follow-on table acquisition.
///
/// @author Steve Ebersole
public final class CompositeIdentifierLockResult implements DomainResult<Object> {

	private final TableDetails table;
	private final String entityName;
	private final NavigablePath path;
	private final JavaType<Object> identifierJavaType;
	private final JavaType<Object>[] jdbcJavaTypes;
	private final int[] positions;
	private final Map<JdbcKey, EntityKey> identities = new HashMap<>();
	private final Map<EntityKey, JdbcKey> expectedValues = new HashMap<>();

	@SuppressWarnings("unchecked")
	public CompositeIdentifierLockResult(
			TableDetails table,
			CompositeIdentifierMapping identifier,
			TableReference tableReference,
			NavigablePath path,
			DomainResultCreationState creationState,
			List<EntityKey> expectedKeys,
			SharedSessionContractImplementor session) {
		this.table = table;
		this.entityName = identifier.findContainingEntityMapping().getEntityName();
		this.path = path;
		identifierJavaType = (JavaType<Object>) identifier.getMappedIdEmbeddableTypeDescriptor().getJavaType();
		final var keyDetails = table.getKeyDetails();
		positions = new int[keyDetails.getColumnCount()];
		jdbcJavaTypes = new JavaType[positions.length];
		final var sqlState = creationState.getSqlAstCreationState();
		final var resolver = sqlState.getSqlExpressionResolver();
		for ( int i = 0; i < positions.length; i++ ) {
			final var column = keyDetails.getKeyColumn( i );
			jdbcJavaTypes[i] = (JavaType<Object>) column.getJdbcMapping().getJdbcJavaType();
			positions[i] = resolver.resolveSqlSelection(
					resolver.resolveSqlExpression( tableReference, column ), jdbcJavaTypes[i], null,
					sqlState.getCreationContext().getTypeConfiguration()
			).getValuesArrayPosition();
		}
		for ( var entityKey : expectedKeys ) {
			if ( !expectedValues.containsKey( entityKey ) ) {
				final var values = new Object[positions.length];
				identifier.breakDownJdbcValues( entityKey.getIdentifier(),
						(index, value, mapping) -> values[index] = value, session );
				final var key = new JdbcKey( values );
				final var previous = identities.putIfAbsent( key, entityKey );
				if ( previous != null && !previous.equals( entityKey ) ) {
					throw identityFailure( "Distinct expected identities have the same relational key" );
				}
				expectedValues.put( entityKey, key );
			}
		}
	}

	/// Uses the same stable relational values for predicates and result correlation.
	void forEachJdbcValue(EntityKey entityKey, ModelPart.JdbcValueConsumer consumer) {
		final var key = expectedValues.get( entityKey );
		for ( int i = 0; i < positions.length; i++ ) {
			consumer.consume( i, key.values[i], table.getKeyDetails().getKeyColumn( i ) );
		}
	}

	@Override
	public String getResultVariable() {
		return null;
	}

	@Override
	public JavaType<?> getResultJavaType() {
		return identifierJavaType;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return path;
	}

	@Override
	public void collectValueIndexesToCache(BitSet valueIndexes) {
		for ( int position : positions ) {
			valueIndexes.set( position );
		}
	}

	@Override
	public DomainResultAssembler<Object> createResultAssembler(InitializerParent<?> parent, AssemblerCreationState creationState) {
		return new DomainResultAssembler<>() {
			@Override
			public Object assemble(RowProcessingState rowProcessingState) {
				final var values = new Object[positions.length];
				for ( int i = 0; i < positions.length; i++ ) {
					values[i] = rowProcessingState.getJdbcValue( positions[i] );
				}
				final var entityKey = identities.get( new JdbcKey( values ) );
				if ( entityKey == null ) {
					throw identityFailure( "Acquired relational key does not match an expected identity" );
				}
				return entityKey.getIdentifier();
			}

			@Override
			public JavaType<Object> getAssembledJavaType() {
				return identifierJavaType;
			}
		};
	}

	private AssertionFailure identityFailure(String message) {
		return new AssertionFailure( message + " for entity '" + entityName
				+ "' during follow-on locking of table '" + table.getTableName() + "'" );
	}

	private final class JdbcKey {
		private final Object[] values;
		private final int hashCode;

		JdbcKey(Object[] values) {
			this.values = new Object[values.length];
			int hash = 1;
			for ( int i = 0; i < values.length; i++ ) {
				final var value = jdbcJavaTypes[i].getMutabilityPlan().deepCopy( values[i] );
				this.values[i] = value;
				hash = 31 * hash + (value == null ? 0 : jdbcJavaTypes[i].extractHashCode( value ));
			}
			hashCode = hash;
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public boolean equals(Object other) {
			if ( this == other ) {
				return true;
			}
			if ( !(other instanceof CompositeIdentifierLockResult.JdbcKey key) || values.length != key.values.length ) {
				return false;
			}
			for ( int i = 0; i < values.length; i++ ) {
				if ( !jdbcJavaTypes[i].areEqual( values[i], key.values[i] ) ) {
					return false;
				}
			}
			return true;
		}
	}
}
