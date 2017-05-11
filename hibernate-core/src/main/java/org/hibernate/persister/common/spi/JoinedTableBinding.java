/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import org.hibernate.mapping.Join;

/**
 * Used to represent both secondary tables and table joins used in joined inheritance.
 *
 * @author Steve Ebersole
 */
public class JoinedTableBinding {
	private final Table referringTable;
	private final Table targetTable;
	private final ForeignKey.ColumnMappings joinPredicateColumnMappings;

	private final boolean optional;

	public JoinedTableBinding(
			Table referringTable,
			Table targetTable,
			ForeignKey.ColumnMappings joinPredicateColumnMappings,
			boolean optional) {
		// this ctor represents the alternative discussed in the other ctor which
		//		assumes that we already have reference to both Tables when we create this.  This
		// 		should always be the case, but you never know...
		// todo (6.0) - pick this ^^ or the other option
		//
		// todo (6.0) : kind of unrelated, but...
		// make sure that the mapping-model relational view (MappedTable e.g.) gets completely transformed/transferred
		// 		into the persister-model's view as the very first step in creation of persisters

		this.referringTable = referringTable;
		this.targetTable = targetTable;
		this.joinPredicateColumnMappings = joinPredicateColumnMappings;
		this.optional = optional;
	}

	public JoinedTableBinding(Join secondaryTableJoinMapping, Table targetTable, boolean optional) {
		this.targetTable = targetTable;
		this.optional = optional;

		// todo (6.0) : resolve/create the "referring table" (the joined table) based on the passed secondaryTableJoinMapping's mapped table
		// secondaryTableJoinMapping.getMappedTable()...
		this.referringTable = null;

		// todo (6.0) : resolve the FK column mappings
		this.joinPredicateColumnMappings = null;

		// todo (6.0) : another option is to have persister creation process create the actual persister relational references
		// 		- which can then just be passed in here
		//		- This should work: the mapping model creation already creates the mapping-model's relational objects
		//			into Database.  The very first step of creating persisters really ought to be going through
		//			Database and creating this persister-level relational model, if that is not whah happen atm.
		//
		// 			BTW, this would allow schema export to happen based on the persister-level relational model, which
		//			has a lot of other benefits/  One of those benefits is not handling "logical name" -> "physical name"
		// 			transformations (PhysicalNamingStrategy) until we get to building this persister-level
		// 			relational model.  The mapping model would contain just the logical name; the persister model would
		//			contain boh the physical name (for SQL statements) as well as its logical name (used to uniquely
		//			identify the table at the mapping-level.
		//
		//			This process would have already created the ForeignKey (predicateSource) as well.  In other
		//			words all the information modeled here can be passed in at ctor.
	}

	public Table getReferringTable() {
		return referringTable;
	}

	public Table getTargetTable() {
		return targetTable;
	}

	public ForeignKey.ColumnMappings getJoinPredicateColumnMappings() {
		return joinPredicateColumnMappings;
	}

	public boolean isOptional() {
		return optional;
	}
}
