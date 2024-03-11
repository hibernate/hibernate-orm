/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.hibernate.FetchMode;
import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;
import org.hibernate.type.MetaType;
import org.hibernate.type.Type;

/**
 * A mapping model object which represents something that's persisted "by value",
 * instead of "by reference", that is, anything with no primary key.
 * <p>
 * A {@code Value} is essentially a Hibernate {@link Type}, together with zero or
 * more {@link Column columns}. In the mapping model, a {@code Value} always comes
 * wrapped in something with higher-level semantics, for example, a property, a
 * collection, or a class.
 *
 * @author Gavin King
 */
public interface Value extends Serializable {

	/**
	 * The number of columns and formulas in the mapping.
	 */
	int getColumnSpan();

	/**
	 * The mapping to columns and formulas.
	 */
	List<Selectable> getSelectables();

	/**
	 * If the mapping involves only columns, return them.
	 *
	 * @throws org.hibernate.AssertionFailure if the mapping involves formulas
	 */
	List<Column> getColumns();

	/**
	 * Same as {@link #getSelectables()} except it returns the PK for the
	 * non-owning side of a one-to-one association.
	 */
	default List<Selectable> getVirtualSelectables() {
		return getSelectables();
	}

	/**
	 * Same as {@link #getColumns()} except it returns the PK for the
	 * non-owning side of a one-to-one association.
	 *
	 * @throws org.hibernate.AssertionFailure if the mapping involves formulas
	 */
	default List<Column> getConstraintColumns() {
		return getColumns();
	}

	Type getType() throws MappingException;

	@Incubating
	default JdbcMapping getSelectableType(Mapping factory, int index) throws MappingException {
		return getType( factory, getType(), index );
	}

	private JdbcMapping getType(Mapping factory, Type elementType, int index) {
		if ( elementType instanceof CompositeType ) {
			final Type[] subtypes = ( (CompositeType) elementType ).getSubtypes();
			for ( int i = 0; i < subtypes.length; i++ ) {
				final Type subtype = subtypes[i];
				final int columnSpan;
				if ( subtype instanceof EntityType ) {
					final EntityType entityType = (EntityType) subtype;
					final Type idType = getIdType( entityType );
					columnSpan = idType.getColumnSpan( factory );
				}
				else {
					columnSpan = subtype.getColumnSpan( factory );
				}
				if ( columnSpan < index ) {
					index -= columnSpan;
				}
				else if ( columnSpan != 0 ) {
					return getType( factory, subtype, index );
				}
			}
			// Should never happen
			throw new IllegalStateException( "Type index is past the types column span!" );
		}
		else if ( elementType instanceof EntityType ) {
			final EntityType entityType = (EntityType) elementType;
			final Type idType = getIdType( entityType );
			return getType( factory, idType, index );
		}
		else if ( elementType instanceof MetaType ) {
			return (JdbcMapping) ( (MetaType) elementType ).getBaseType();
		}
		return (JdbcMapping) elementType;
	}

	private Type getIdType(EntityType entityType) {
		final PersistentClass entityBinding = getBuildingContext().getMetadataCollector()
				.getEntityBinding( entityType.getAssociatedEntityName() );
		final Type idType;
		if ( entityType.isReferenceToPrimaryKey() ) {
			idType = entityBinding.getIdentifier().getType();
		}
		else {
			idType = entityBinding.getProperty( entityType.getRHSUniqueKeyPropertyName() ).getType();
		}
		return idType;
	}

	FetchMode getFetchMode();

	Table getTable();

	boolean hasFormula();

	boolean isAlternateUniqueKey();

	boolean isNullable();

	void createForeignKey();

	// called when this is the foreign key of a
	// @OneToOne with a FK, or a @OneToMany with
	// a join table
	void createUniqueKey(MetadataBuildingContext context);

	boolean isSimpleValue();

	boolean isValid(Mapping mapping) throws MappingException;

	void setTypeUsingReflection(String className, String propertyName) throws MappingException;

	Object accept(ValueVisitor visitor);

	boolean isSame(Value other);

	boolean[] getColumnInsertability();
	boolean hasAnyInsertableColumns();

	boolean[] getColumnUpdateability();
	boolean hasAnyUpdatableColumns();

	@Incubating
	default MetadataBuildingContext getBuildingContext() {
		throw new UnsupportedOperationException( "Value#getBuildingContext is not implemented by: " + getClass().getName() );
	}
	ServiceRegistry getServiceRegistry();
	Value copy();

	boolean isColumnInsertable(int index);

	boolean isColumnUpdateable(int index);

	@Incubating
	default String getExtraCreateTableInfo() {
		return "";
	}

	/**
	 * Checks if this value contains any duplicate column. A column
	 * is considered duplicate when its {@link Column#getName() name} is
	 * already contained in the {@code distinctColumn} set.
	 * <p>
	 * If a duplicate column is found, a {@link MappingException} is thrown.
	 *
	 * @param distinctColumns set containing the names of the columns to check
	 * @param owner the owner of this value, used just for error reporting
	 */
	@Internal
	default void checkColumnDuplication(Set<String> distinctColumns, String owner) {
		for ( int i = 0; i < getSelectables().size(); i++ ) {
			final Selectable selectable = getSelectables().get( i );
			if ( isColumnInsertable( i ) || isColumnUpdateable( i ) ) {
				final Column col = (Column) selectable;
				if ( !distinctColumns.add( col.getName() ) ) {
					throw new MappingException(
							"Column '" + col.getName()
									+ "' is duplicated in mapping for " + owner
									+ " (use '@Column(insertable=false, updatable=false)' when mapping multiple properties to the same column)"
					);
				}
			}
		}
	}
}
