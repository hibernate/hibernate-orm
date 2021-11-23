/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.internal.idtable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Contributable;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Value;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;

/**
 * @author Steve Ebersole
 */
public class IdTable implements Exportable, Contributable {

	public static final String DEFAULT_ALIAS = "idtable_";

	private final EntityMappingType entityDescriptor;
	private final String qualifiedTableName;

	private IdTableSessionUidColumn sessionUidColumn;
	private final List<IdTableColumn> columns = new ArrayList<>();

	private final Dialect dialect;

	public IdTable(
			EntityMappingType entityDescriptor,
			Function<String, String> idTableNameAdjuster,
			Dialect dialect,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		this.entityDescriptor = entityDescriptor;

		// The table name might be a sub-query, which is inappropriate for an id table name
		final String originalTableName = entityDescriptor.getEntityPersister().getSynchronizedQuerySpaces()[0];
		if ( Identifier.isQuoted( originalTableName ) ) {
			this.qualifiedTableName = dialect.quote( idTableNameAdjuster.apply( Identifier.unQuote( originalTableName ) ) );
		}
		else {
			this.qualifiedTableName = idTableNameAdjuster.apply( originalTableName );
		}

		final PersistentClass entityBinding = runtimeModelCreationContext.getBootModel()
				.getEntityBinding( entityDescriptor.getEntityName() );

		final Iterator<Column> itr = entityBinding.getTable().getPrimaryKey().getColumnIterator();
		final Iterator<JdbcMapping> jdbcMappings = entityDescriptor.getIdentifierMapping().getJdbcMappings().iterator();
		while ( itr.hasNext() ) {
			final Column column = itr.next();
			final JdbcMapping jdbcMapping = jdbcMappings.next();
			columns.add(
					new IdTableColumn(
							this,
							column.getText( dialect ),
							jdbcMapping,
							column.getSqlType( dialect, runtimeModelCreationContext.getMetadata() )
					)
			);
		}

		entityDescriptor.visitSubTypeAttributeMappings(
				attribute -> {
					if ( attribute instanceof PluralAttributeMapping ) {
						final PluralAttributeMapping pluralAttribute = (PluralAttributeMapping) attribute;

						if ( pluralAttribute.getSeparateCollectionTable() != null ) {
							// Ensure that the FK target columns are available
							final ModelPart fkTarget = pluralAttribute.getKeyDescriptor().getTargetPart();
							if ( !( fkTarget instanceof EntityIdentifierMapping ) ) {
								final Value value = entityBinding.getSubclassProperty( pluralAttribute.getAttributeName() )
										.getValue();
								final Iterator<Selectable> columnIterator = ( (Collection) value ).getKey()
										.getColumnIterator();
								fkTarget.forEachSelectable(
										(columnIndex, selection) -> {
											final Selectable selectable = columnIterator.next();
											if ( selectable instanceof Column ) {
												columns.add(
														new IdTableColumn(
																this,
																selectable.getText( dialect ),
																selection.getJdbcMapping(),
																( (Column) selectable ).getSqlType(
																		dialect,
																		runtimeModelCreationContext.getMetadata()
																)
														)
												);
											}
										}
								);
							}
						}
					}
				}
		);

		this.dialect = dialect;
	}

	public EntityMappingType getEntityDescriptor() {
		return entityDescriptor;
	}

	public String getQualifiedTableName() {
		return qualifiedTableName;
	}

	public List<IdTableColumn> getIdTableColumns() {
		return columns;
	}

	public IdTableSessionUidColumn getSessionUidColumn() {
		return sessionUidColumn;
	}

	public String getTableExpression() {
		return qualifiedTableName;
	}

	public void addColumn(IdTableColumn column) {
		columns.add( column );
		if ( column instanceof IdTableSessionUidColumn ) {
			this.sessionUidColumn = (IdTableSessionUidColumn) column;
		}
	}

	@Override
	public String getContributor() {
		return entityDescriptor.getContributor();
	}

	@Override
	public String getExportIdentifier() {
		return getQualifiedTableName();
	}

	public Dialect getDialect() {
		return this.dialect;
	}
}
