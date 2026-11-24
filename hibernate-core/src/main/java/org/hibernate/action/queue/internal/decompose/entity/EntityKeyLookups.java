package org.hibernate.action.queue.internal.decompose.entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.action.queue.internal.decompose.KeyLookup;
import org.hibernate.action.queue.internal.decompose.KeyLookupStatements;
import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.ast.spi.model.TableMutation;
import org.hibernate.sql.spi.mutation.ValuesAnalysis;

/// The foreign keys which the inserts, or the updates, of an entity may read from
/// the rows they reference, and the statements which do so.
///
/// @see KeyLookup
final class EntityKeyLookups {
	/// The mutation of one row of a table of the entity.
	interface Row {
		/// The table written, which may differ from the one being planned.
		EntityTableDescriptor getTableDescriptor();

		/// The value of the attribute at the given position in the table, or
		/// `null` if the mutation does not write it.
		Object writtenValue(int attribute, SharedSessionContractImplementor session);

		/// Bind for the statement which reads foreign keys from the given rows.
		void readKeysFrom(KeyLookup.Reference[] references);
	}

	private final MutationKind kind;
	private final SessionFactoryImplementor factory;
	private final Map<String, KeyLookup[]> lookups = new ConcurrentHashMap<>();
	private final Map<String, KeyLookupStatements> staticStatements = new ConcurrentHashMap<>();

	EntityKeyLookups(MutationKind kind, SessionFactoryImplementor factory) {
		this.kind = kind;
		this.factory = factory;
	}

	/// The lookups of a table, by position of the attribute. Empty if it has none.
	KeyLookup[] of(EntityTableDescriptor table) {
		return lookups.computeIfAbsent( table.name(),
				name -> KeyLookup.forAttributes( table, kind == MutationKind.INSERT, factory ) );
	}

	/// Choose the statement which reads foreign keys from the rows they reference,
	/// when the row references rows whose keys are not in memory. The row then
	/// binds for that statement.
	///
	/// @param isStatic whether the mutation is the same for every row of the table
	///
	/// @return `null` if the mutation should be performed as given
	KeyLookupStatements.Statement resolve(
			KeyLookup[] lookups,
			Row row,
			EntityTableDescriptor table,
			TableMutation<?> mutation,
			boolean isStatic,
			ValuesAnalysis valuesAnalysis,
			SharedSessionContractImplementor session) {
		KeyLookup.Reference[] references = null;
		for ( int i = 0; i < lookups.length; i++ ) {
			final var reference = lookups[i] == null ? null
					: lookups[i].resolve( row.writtenValue( i, session ), session );
			if ( reference != null ) {
				if ( references == null ) {
					references = new KeyLookup.Reference[lookups.length];
				}
				references[i] = reference;
			}
		}
		if ( references == null ) {
			return null;
		}
		final var shapes = KeyLookup.shapes( references );
		final var statement = isStatic
				? staticStatements.computeIfAbsent( table.name(),
						name -> new KeyLookupStatements( mutation, table, kind, factory ) ).resolve( shapes )
				: KeyLookupStatements.derive( mutation, shapes, valuesAnalysis, table, kind, factory );
		if ( statement != null ) {
			row.readKeysFrom( references );
		}
		return statement;
	}
}
