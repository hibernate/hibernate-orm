package org.hibernate.action.queue.internal.decompose;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.hibernate.Hibernate;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.StaleStateException;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.dialect.jdbc.spi.InsertSelectExecution;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.SelectableMapping;

import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptableOrNull;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/// A foreign key which references a unique key of its target instead of the primary
/// key, and whose columns an `insert` or `update` may read from the referenced row.
///
/// When the target is known by its primary key but its unique key is unfetched, the
/// statement reads the key from the referenced row. Otherwise, Hibernate would have
/// to load the target just to copy the key.
///
/// An instance describes the foreign key, and is created once for the table which
/// holds it. A [Reference] is its use by one row, resolved while a flush is planned.
public final class KeyLookup {
	private static final KeyLookup[] NONE = new KeyLookup[0];

	private final ParameterUsage usage;
	private final EntityMappingType target;
	private final EntityIdentifierMapping identifier;
	private final String targetPartName;
	private final SelectableMapping[] keyColumns;
	private final SelectableMapping[] targetColumns;
	// The attribute of the target which holds each column, when every column has one
	private final AttributeMapping[] targetAttributes;
	private final String[] parameterNames;
	private final Shape unfetched;

	private KeyLookup(
			ParameterUsage usage,
			EntityMappingType target,
			String targetPartName,
			SelectableMapping[] keyColumns,
			SelectableMapping[] targetColumns) {
		this.usage = usage;
		this.target = target;
		this.identifier = target.getIdentifierMapping();
		this.targetPartName = targetPartName;
		this.keyColumns = keyColumns;
		this.targetColumns = targetColumns;
		this.targetAttributes = attributesHolding( targetColumns, target );
		this.parameterNames = new String[identifier.getJdbcTypeCount()];
		for ( int i = 0; i < parameterNames.length; i++ ) {
			// Not a column of the mutated table, so the name cannot collide with one
			parameterNames[i] = keyColumns[0].getSelectionExpression() + "->"
					+ identifier.getSelectable( i ).getSelectionExpression();
		}
		this.unfetched = new Shape( this, ( 1 << keyColumns.length ) - 1 );
	}

	/// The lookup for a foreign key held by the given table.
	///
	/// @param usage whether the statement assigns the key, or restricts by it
	///
	/// @return `null` if a statement can never read this key from the referenced row
	public static KeyLookup of(EntityAssociationMapping association, String table, ParameterUsage usage) {
		if ( association == null
				|| association.getSideNature() != ForeignKeyDescriptor.Nature.KEY
				|| association.isReferenceToPrimaryKey() ) {
			return null;
		}
		final var foreignKey = association.getForeignKeyDescriptor();
		final var target = association.getAssociatedEntityMappingType();
		final var persister = target.getEntityPersister();
		final String targetTable = foreignKey.getTargetTable();
		final int columnCount = foreignKey.getJdbcTypeCount();
		if ( columnCount >= Integer.SIZE
				|| !foreignKey.getKeyTable().equals( table )
				|| !targetTable.equals( target.getIdentifierMapping().getContainingTableExpression() )
				|| targetTable.equals( table )
				// The statement reads the referenced row directly, so the row must be
				// visible to it exactly when the entity is visible to the session.
				|| persister.hasNamedQueryLoader()
				|| target.hasWhereRestrictions()
				|| target.getSoftDeleteMapping() != null
				|| target.getTemporalMapping() != null
				|| target.getTenantIdMapping() != null
				|| persister.isInherited()
				|| target.hasSubclasses() ) {
			return null;
		}
		final var keyColumns = new SelectableMapping[columnCount];
		final var targetColumns = new SelectableMapping[columnCount];
		foreignKey.visitTargetSelectables( (index, selectable) -> targetColumns[index] = selectable );
		for ( int i = 0; i < columnCount; i++ ) {
			keyColumns[i] = foreignKey.getSelectable( i );
			if ( keyColumns[i].isFormula()
					|| targetColumns[i].isFormula()
					|| !targetColumns[i].getContainingTableExpression().equals( targetTable ) ) {
				return null;
			}
		}
		return new KeyLookup( usage, target, foreignKey.getTargetPart().getPartName(), keyColumns, targetColumns );
	}

	/// The lookups for the to-one attributes written to a table of an entity, in the
	/// order of the table's attributes.
	///
	/// @param insert whether the table is being inserted to, or updated
	///
	/// @return an empty array if no statement on the table can read a key from a referenced row
	public static KeyLookup[] forAttributes(EntityTableDescriptor table, boolean insert, SessionFactoryImplementor factory) {
		final var attributes = table.attributes();
		KeyLookup[] lookups = NONE;
		for ( int i = 0; i < attributes.size(); i++ ) {
			final var attribute = attributes.get( i );
			if ( !attribute.isPluralAttributeMapping() && attribute.getGenerator() == null ) {
				final var lookup = of( attribute.asEntityAssociationMapping(), table.name(), ParameterUsage.SET );
				if ( lookup != null && lookup.isWritten( table, insert )
						// An update of a unique column may have its value nulled and
						// assigned again later, which needs the value in memory.
						&& ( insert || !lookup.isUnique( table, factory ) ) ) {
					if ( lookups == NONE ) {
						lookups = new KeyLookup[attributes.size()];
					}
					lookups[i] = lookup;
				}
			}
		}
		return lookups;
	}

	/// Whether a statement on the table writes every column from the attribute,
	/// and not from the identifier with which the key may share a column.
	private boolean isWritten(EntityTableDescriptor table, boolean insert) {
		for ( var column : keyColumns ) {
			if ( !( insert ? column.isInsertable() : column.isUpdateable() ) ) {
				return false;
			}
			for ( var key : table.keyDescriptor().columns() ) {
				if ( key.selectionExpression().equals( column.getSelectionExpression() ) ) {
					return false;
				}
			}
		}
		return true;
	}

	private boolean isUnique(EntityTableDescriptor table, SessionFactoryImplementor factory) {
		final var constraints = factory.getMappingMetamodel().getConstraintModel().getUniqueConstraintsForTable( table.name() );
		if ( constraints != null ) {
			for ( var constraint : constraints ) {
				final var columns = constraint.columns();
				for ( int i = 0; i < columns.getJdbcTypeCount(); i++ ) {
					for ( var column : keyColumns ) {
						if ( column.getSelectionExpression().equals( columns.getSelectable( i ).getSelectionExpression() ) ) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	/// Whether an insert which joins the JDBC batch, when the session has one, may
	/// read its row from a select.
	public static boolean canInsertInBatch(SharedSessionContractImplementor session) {
		final Integer batchSize = session.getConfiguredJdbcBatchSize();
		return batchSize == null || batchSize < 2
			|| session.getJdbcServices().getDialect().supports( InsertSelectExecution.BATCH );
	}

	/// The shapes of the given references, in order.
	///
	/// @param references by position, with `null` where a key is bound
	public static List<Shape> shapes(Reference[] references) {
		return Arrays.stream( references ).filter( Objects::nonNull ).map( Reference::shape ).toList();
	}

	/// Check that the rows read by a statement exist, if it matched no row or
	/// JDBC could not report whether it did.
	public static void verify(Reference[] references, int rowCount) {
		if ( rowCount <= 0 ) {
			for ( var reference : references ) {
				if ( reference != null ) {
					reference.verify();
				}
			}
		}
	}

	/// Check the row count of a statement which must match a row, having read
	/// keys from the given rows.
	public static boolean checkMatched(Reference[] references, int rowCount, String sql) {
		verify( references, rowCount );
		if ( rowCount == 0 ) {
			throw new StaleStateException( "No row matched " + sql );
		}
		return true;
	}

	private static AttributeMapping[] attributesHolding(SelectableMapping[] columns, EntityMappingType target) {
		final var mappings = target.getAttributeMappings();
		final var attributes = new AttributeMapping[columns.length];
		for ( int i = 0; i < columns.length; i++ ) {
			for ( int j = 0; j < mappings.size() && attributes[i] == null; j++ ) {
				final var basic = mappings.get( j ).asBasicValuedModelPart();
				if ( basic != null && !basic.isFormula()
						&& basic.getSelectionExpression().equals( columns[i].getSelectionExpression() )
						&& basic.getContainingTableExpression().equals( columns[i].getContainingTableExpression() ) ) {
					attributes[i] = mappings.get( j );
				}
			}
			if ( attributes[i] == null ) {
				// The key is not a flat list of basic attributes, so it is fetched as a whole
				return null;
			}
		}
		return attributes;
	}

	/// Whether the statement assigns the key, or restricts by it.
	public ParameterUsage usage() {
		return usage;
	}

	public EntityIdentifierMapping identifier() {
		return identifier;
	}

	public int columnCount() {
		return keyColumns.length;
	}

	/// A column of the foreign key, in the mutated table.
	public SelectableMapping keyColumn(int index) {
		return keyColumns[index];
	}

	/// The column of the referenced row which supplies the given key column.
	public SelectableMapping targetColumn(int index) {
		return targetColumns[index];
	}

	/// The name under which a column of the target's primary key is bound.
	public String parameterName(int identifierColumn) {
		return parameterNames[identifierColumn];
	}

	/// The use of this lookup by a row which references the given target.
	///
	/// @return `null` if the key is available in memory, or cannot be read by the statement
	public Reference resolve(Object value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		final int missing = missingColumns( value );
		if ( missing == 0 || target.isAffectedByEnabledFilters( session.getLoadQueryInfluencers(), true ) ) {
			return null;
		}
		final var proxy = extractLazyInitializer( value );
		final Object entity = proxy == null || proxy.isUninitialized() ? value : proxy.getImplementation();
		final var interceptable = asPersistentAttributeInterceptableOrNull( entity );
		if ( interceptable != null
				&& interceptable.$$_hibernate_getInterceptor() instanceof EnhancementAsProxyLazinessInterceptor interceptor
				&& interceptor.hasWrittenFieldNames() ) {
			// The application may have assigned part of the key to this reference
			return null;
		}
		final Object id = proxy == null ? identifier.getIdentifier( value ) : proxy.getInternalIdentifier();
		return id == null
				? null
				: new Reference( missing == unfetched.missing ? unfetched : new Shape( this, missing ), entity, id, session );
	}

	private int missingColumns(Object value) {
		if ( targetAttributes == null ) {
			return Hibernate.isPropertyInitialized( value, targetPartName ) ? 0 : unfetched.missing;
		}
		int missing = 0;
		for ( int i = 0; i < targetAttributes.length; i++ ) {
			if ( !Hibernate.isPropertyInitialized( value, targetAttributes[i].getAttributeName() ) ) {
				missing |= 1 << i;
			}
		}
		return missing;
	}

	/// Which columns of a foreign key a statement reads from the referenced row.
	/// Statements with equal shapes have the same SQL.
	public record Shape(KeyLookup lookup, int missing) {
		public boolean isMissing(int column) {
			return ( missing & 1 << column ) != 0;
		}
	}

	/// The use of a lookup by one row.
	public static final class Reference {
		private final Shape shape;
		private final Object entity;
		private final Object id;
		private final SharedSessionContractImplementor session;

		private Reference(Shape shape, Object entity, Object id, SharedSessionContractImplementor session) {
			this.shape = shape;
			this.entity = entity;
			this.id = id;
			this.session = session;
		}

		public Shape shape() {
			return shape;
		}

		/// Bind the primary key of the referenced row, and any key column held in memory.
		public void bind(JdbcValueBindings bindings) {
			final var lookup = shape.lookup;
			lookup.identifier.forEachJdbcValue( id, bindings, lookup,
					(index, target, source, value, mapping) ->
							target.bindValue( value, source.parameterNames[index], ParameterUsage.RESTRICT ),
					session );
			for ( int i = 0; i < lookup.keyColumns.length; i++ ) {
				if ( !shape.isMissing( i ) ) {
					final var attribute = lookup.targetAttributes[i];
					bindings.bindValue(
							attribute.disassemble( attribute.getValue( entity ), session ),
							lookup.keyColumns[i].getSelectionExpression(),
							lookup.usage
					);
				}
			}
		}

		/// Check that the referenced row exists, after the statement matched no row
		/// or JDBC could not report whether it did.
		private void verify() {
			final var persister = shape.lookup.target.getEntityPersister();
			if ( persister.getDatabaseSnapshot( id, session ) == null ) {
				session.getFactory().getEntityNotFoundDelegate().handleEntityNotFound( persister.getEntityName(), id );
				throw new ObjectNotFoundException( id, persister.getEntityName() );
			}
		}
	}
}
