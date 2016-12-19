/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.predicate;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.sql.sqm.convert.spi.SqlTreeWalker;


/**
 * @author Steve Ebersole
 */
public class Junction implements Predicate {
	public enum Nature {
		/**
		 * An AND
		 */
		CONJUNCTION,
		/**
		 * An OR
		 */
		DISJUNCTION
	}

	private final Nature nature;
	private final List<Predicate> predicates = new ArrayList<>();

	public Junction(Nature nature) {
		this.nature = nature;
	}

	public void add(Predicate predicate) {
		predicates.add( predicate );
	}

	public Nature getNature() {
		return nature;
	}

	public List<Predicate> getPredicates() {
		return predicates;
	}

	@Override
	public boolean isEmpty() {
		return predicates.isEmpty();
	}

	@Override
	public void accept(SqlTreeWalker sqlTreeWalker) {
		sqlTreeWalker.visitJunction( this );
	}
}
