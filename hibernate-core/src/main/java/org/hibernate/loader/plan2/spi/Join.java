/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader.plan2.spi;

/**
 * Represents a join in the QuerySpace-sense.  In HQL/JP-QL, this would be an implicit/explicit join; in
 * metamodel-driven LoadPlans, this would be joins indicated by the metamodel.
 */
public interface Join {
	// todo : would be good to have the SQL alias info here because we know it when we would be building this Join,
	// and to do it afterwards would require lot of logic to recreate.
	// But we do want this model to be workable in Search/OGM as well, plus the HQL parser has shown time-and-again
	// that it is best to put off resolving and injecting physical aliases etc until as-late-as-possible.

	// todo : do we know enough here to declare the "owner" side?  aka, the "fk direction"
	// and if we do ^^, is that enough to figure out the SQL aliases more easily (see above)?

	public QuerySpace getLeftHandSide();

	public QuerySpace getRightHandSide();

	public boolean isRightHandSideOptional();
}
