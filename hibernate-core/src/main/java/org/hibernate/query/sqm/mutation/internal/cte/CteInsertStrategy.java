/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.cte;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SingleTableSubclass;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.tree.insert.SqmInsertStatement;
import org.hibernate.sql.ast.tree.cte.CteTable;

/**
 * @asciidoc
 *
 * {@link SqmMultiTableInsertStrategy} implementation using SQL's modifiable CTE (Common Table Expression)
 * approach to perform the update/delete.  E.g. (using delete):
 *
 * This strategy will create a query like this:
 *
 * ```
 * with hte_entity as (
 * 	select *, next value for sequence from ...
 * ),
 * dml_cte_1 as (
 * 	insert into base_table select e.id, e.base from hte_entity e
 * 	returning id
 * ),
 * dml_cte_2 as (
 *  insert into sub_table select e.id, e.sub1 from hte_entity e
 * 	returning id
 * )
 * select count(*) from dml_cte_1
 * ```
 *
 * if the sequence generator has an optimizer, the optimizer is implemented in SQL like this:
 *
 * ```
 * with hte_entity_raw as (
 * 	select *, row_number() over() from ...
 * ),
 * rows_with_next_val(rn, val) as (
 *  -- then, fetch a sequence value for every row number that needs it
 *  select rn, next value for sequence FROM rows_needing_next_val
 *  where (e.rn-1) % [incrementSize] = 0
 * ),
 * hte_entity as (
 *  select e.*, t.val + (e.rn - t.rn) as id
 *  from hte_entity_raw e
 *  -- join the data against the generated sequence value, based on the row number group they belong to
 *  -- i.e. where the row number is within the increment size
 *  left join rows_with_next_val t ON e.rn - ((e.rn-1) % 10) = t.rn
 * ),
 * dml_cte_1 as (
 * 	insert into base_table select e.id, e.base from hte_entity e
 * 	returning id
 * ),
 * dml_cte_2 as (
 *  insert into sub_table select e.id, e.sub1 from hte_entity e
 * 	returning id
 * )
 * select count(*) from dml_cte_1
 * ```
 *
 * in case the id generator uses identity generation, a row number will be created which should ensure insert ordering
 *
 * ```
 * with hte_entity_raw as (
 * 	select *, row_number() over() from ...
 * ),
 * dml_cte_1 as (
 * 	insert into base_table select e.id, e.base from hte_entity_raw e
 * 	returning id, e.row_number
 * ),
 * with hte_entity as (
 * 	select * from hte_entity e join dml_cte_1 c on e.row_number = c.row_number
 * ),
 * dml_cte_2 as (
 *  insert into sub_table select c.id, e.sub1 from hte_entity e
 * 	returning id
 * )
 * select count(*) from dml_cte_1
 * ```
 *
 * @author Christian Beikov
 */
public class CteInsertStrategy implements SqmMultiTableInsertStrategy {
	public static final String SHORT_NAME = "cte";

	private final EntityPersister rootDescriptor;
	private final SessionFactoryImplementor sessionFactory;
	private final CteTable entityCteTable;

	public CteInsertStrategy(
			EntityMappingType rootEntityType,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		this( rootEntityType.getEntityPersister(), runtimeModelCreationContext );
	}

	public CteInsertStrategy(
			EntityPersister rootDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		this.rootDescriptor = rootDescriptor;
		this.sessionFactory = runtimeModelCreationContext.getSessionFactory();

		final Dialect dialect = runtimeModelCreationContext.getDialect();

		if ( !dialect.supportsNonQueryWithCTE() ) {
			throw new UnsupportedOperationException(
					getClass().getSimpleName() +
							" can only be used with Dialects that support CTE that can take UPDATE or DELETE statements as well"
			);
		}

		if ( !dialect.supportsValuesList() ) {
			throw new UnsupportedOperationException(
					getClass().getSimpleName() +
							" can only be used with Dialects that support VALUES lists"
			);
		}

		final PersistentClass persistentClass = runtimeModelCreationContext.getMetadata()
				.getEntityBinding( rootDescriptor.getEntityName() );
		final Identifier tableNameIdentifier;
		if ( persistentClass instanceof SingleTableSubclass ) {
			// In this case, the descriptor is a subclass of a single table inheritance.
			// To avoid name collisions, we suffix the table name with the subclass number
			tableNameIdentifier = new Identifier(
					persistentClass.getTable().getNameIdentifier().getText() + persistentClass.getSubclassId(),
					persistentClass.getTable().getNameIdentifier().isQuoted()
			);
		}
		else {
			tableNameIdentifier = persistentClass.getTable().getNameIdentifier();
		}
		final String cteName = TemporaryTable.ENTITY_TABLE_PREFIX + tableNameIdentifier.getText();
		final String qualifiedCteName = new Identifier(
				cteName.substring( 0, Math.min( dialect.getMaxIdentifierLength(), cteName.length() ) ),
				tableNameIdentifier.isQuoted()
		).render( dialect );
		this.entityCteTable = CteTable.createEntityTable( qualifiedCteName, persistentClass );
	}

	@Override
	public int executeInsert(
			SqmInsertStatement<?> sqmInsertStatement,
			DomainParameterXref domainParameterXref,
			DomainQueryExecutionContext context) {
		return new CteInsertHandler( entityCteTable, sqmInsertStatement, domainParameterXref, sessionFactory ).execute( context );
	}

	protected EntityPersister getRootDescriptor() {
		return rootDescriptor;
	}

	protected SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	protected CteTable getEntityCteTable() {
		return entityCteTable;
	}
}
