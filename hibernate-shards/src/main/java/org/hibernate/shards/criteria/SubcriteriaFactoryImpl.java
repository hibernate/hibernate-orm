/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */

package org.hibernate.shards.criteria;

import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.shards.session.ShardedSessionException;
import org.hibernate.sql.JoinType;

/**
 * Concrete implementation of the {@link SubcriteriaFactory} interface.
 * Used to lazily create {@link org.hibernate.internal.CriteriaImpl.Subcriteria}
 *
 * @author maxr@google.com (Max Ross)
 * @see Criteria#createCriteria(String)
 * @see Criteria#createCriteria(String, int)
 * @see Criteria#createCriteria(String, String)
 * @see Criteria#createCriteria(String, String, int)
 */
public class SubcriteriaFactoryImpl implements SubcriteriaFactory {

    private enum MethodSig {
        ASSOCIATION,
        ASSOCIATION_AND_JOIN_TYPE,
        ASSOCIATION_AND_ALIAS,
        ASSOCIATION_AND_ALIAS_AND_JOIN_TYPE,
        ASSOCIATION_AND_ALIAS_AND_JOIN_TYPE_AND_WITH_CLAUSE
    }

    // used to tell us which overload of createCriteria to invoke
    private final MethodSig methodSig;

    // the association we'll pass to createCriteria
    private final String association;

    // the join type we'll pass to createCriteria.  Can be null.
    private final JoinType joinType;

    // the alias we'll pass to createCriteria.  Can be null.
    private final String alias;

    // the criterion to the createCriteria.  Can be null.
    private final Criterion withClause;

    /**
     * Construct a SubcriteriaFactoryImpl
     *
     * @param association the association we'll pass to createCriteria
     */
    public SubcriteriaFactoryImpl(final String association) {
        this(MethodSig.ASSOCIATION, association, JoinType.INNER_JOIN, null, null);
    }

    /**
     * Construct a SubcriteriaFactoryImpl
     *
     * @param associationPath the association we'll pass to createCriteria
     * @param joinType        the join type we'll pass to createCriteria
     */
    public SubcriteriaFactoryImpl(final String associationPath, final JoinType joinType) {
        this(MethodSig.ASSOCIATION_AND_JOIN_TYPE, associationPath, joinType, null, null);
    }

    /**
     * Construct a SubcriteriaFactoryImpl
     *
     * @param associationPath the association we'll pass to createCriteria
     * @param alias           the alias we'll pass to createCriteria
     */
    public SubcriteriaFactoryImpl(final String associationPath, final String alias) {
        this(MethodSig.ASSOCIATION_AND_ALIAS, associationPath, JoinType.INNER_JOIN, alias, null);
    }

    /**
     * Construct a SubcriteriaFactoryImpl
     *
     * @param association the association we'll pass to createCriteria
     * @param alias       the alias we'll pass to createCriteria
     * @param joinType    the join type we'll pass to createCriteria
     */
    public SubcriteriaFactoryImpl(final String association, final String alias, final JoinType joinType) {
        this(MethodSig.ASSOCIATION_AND_ALIAS_AND_JOIN_TYPE, association, joinType, alias, null);
    }

    /**
     * Create a new <tt>Criteria</tt>, "rooted" at the associated entity,
     * assigning the given alias and using the specified join type.
     *
     * @param associationPath A dot-seperated property path
     * @param alias The alias to assign to the joined association (for later reference).
     * @param joinType The type of join to use.
     * @param withClause The criteria to be added to the join condition (<tt>ON</tt> clause)
     *
     * @return the created "sub criteria"
     *
     * @throws org.hibernate.HibernateException Indicates a problem creating the sub criteria
     */
    public SubcriteriaFactoryImpl(final String associationPath, final String alias, final JoinType joinType,
                                  final Criterion withClause) {
        this(MethodSig.ASSOCIATION_AND_ALIAS_AND_JOIN_TYPE_AND_WITH_CLAUSE, associationPath, joinType, alias, withClause);
    }

    /**
     * Construct a SubcriteriaFactoryImpl
     *
     * @param methodSig   used to tell us which overload of createCriteria to invoke
     * @param association the association we'll pass to createCriteria
     * @param joinType    the join type we'll pass to createCriteria.  Can be null.
     * @param alias       the alias we'll pass to createCriteria.  Can be null.
     */
    private SubcriteriaFactoryImpl(final MethodSig methodSig, final String association,
                                   final /*@Nullable*/ JoinType joinType, final /*@Nullable*/ String alias,
                                   final Criterion withClause) {

        this.methodSig = methodSig;
        this.association = association;
        this.joinType = joinType;
        this.alias = alias;
        this.withClause = withClause;
    }

    public Criteria createSubcriteria(Criteria parent, Iterable<CriteriaEvent> events) {

        // call the right overload to actually create the Criteria
        Criteria crit;

        switch (methodSig) {
            case ASSOCIATION:
                crit = parent.createCriteria(association);
                break;
            case ASSOCIATION_AND_JOIN_TYPE:
                crit = parent.createCriteria(association, joinType);
                break;
            case ASSOCIATION_AND_ALIAS:
                crit = parent.createCriteria(association, alias);
                break;
            case ASSOCIATION_AND_ALIAS_AND_JOIN_TYPE:
                crit = parent.createCriteria(association, alias, joinType);
                break;
            case ASSOCIATION_AND_ALIAS_AND_JOIN_TYPE_AND_WITH_CLAUSE:
                crit = parent.createCriteria(association, alias, joinType, withClause);
                break;
            default:
                throw new ShardedSessionException(
                        "Unknown constructor type for subcriteria creation: " + methodSig);
        }

        // apply the events
        for (final CriteriaEvent event : events) {
            event.onEvent(crit);
        }

        return crit;
    }
}
