/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Models a "secondary table" ({@link javax.persistence.SecondaryTable}) binding
 * for an entity.
 *
 * @author Steve Ebersole
 */
public class SecondaryTableBinding implements NonRootTableBinding {
	private final Table targetTable;
	private final boolean optional;


	// todo (6.0) : how to model and expose the join predicate
	//		- one option would be to carry forward from the mapping model the
	//			modeling of a FK and keep a ref to the FK here to define the
	// 			predicate (sans filters, etc)
	//		- another option is to keep an SQM predicate-like tree
	//
	// actually ^^, since both Hibernate and JPA say that secondary tables
	//		implicitly join to the entity's root table via that table's
	//		PK... we may be able to just keep a list of the columns from
	//		the secondary table that make up the secondary table side
	//		of the join predicate
	//
	// btw, both Hibernate and JPA say that this applies to joined inheritance
	// 		tables as well.  Even though that is not really relevant here, it
	//		would be good to have a consistent table, possibly along with a
	//		common contract for consistency: NonRootTableBinding?
	//
	// from email to dev list on subject:
	//		So we have 3 options total for modeling this join predicate:
	//			1) Maintain a predicate tree as part of this SecondaryTableBinding.
	// 				ATM we have no such concept of this either in the runtime metamodel,
	// 				so we would need to add this if we choose this option.  This would
	// 				mean adding the concept of conjunction/disjunction and relational-
	// 				operators in some form to the runtime metamodel.  Personally, this
	// 				is my least favorite option.
	//			2) Maintain the join predicate on SecondaryTableBinding via a FK reference.
	// 				Again, this would mean adding a new concept/class to model the FK as
	// 				part of the runtime metamodel.  I am not against this option so long
	// 				as we deem it has similar benefits in other parts of the codebase -
	// 				I'd prefer to not add such a concept just to handle this case.
	//			3) Follow the assumption regarding the "left hand side" of these joins and
	// 				just keep a list of the columns from the secondary table that link to
	// 				the entity's root table's PK columns.
	//
	// for now, just keep the list of "target table columns"

	private final List<Column> joinColumns = new ArrayList<>();

	public SecondaryTableBinding(Table targetTable, boolean optional) {
		this.targetTable = targetTable;
		this.optional = optional;
	}

	@Override
	public Table getTargetTable() {
		return targetTable;
	}

	@Override
	public boolean isOptional() {
		return optional;
	}

	@Override
	public List<Column> getJoinColumns() {
		return Collections.unmodifiableList( joinColumns );
	}
}
