/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.strategy;

/**
 * Audit strategy which persists and retrieves audit information using a validity algorithm, based on the
 * start-revision and end-revision of a row in the audit tables.
 * <p>This algorithm works as follows:
 * <ul>
 * <li>For a <strong>new row</strong> that is persisted in an audit table, only the <strong>start-revision</strong> column of that row is set</li>
 * <li>At the same time the <strong>end-revision</strong> field of the <strong>previous</strong> audit row is set to this revision</li>
 * <li>Queries are retrieved using 'between start and end revision', instead of a subquery.</li>
 * </ul>
 * </p>
 * <p/>
 * <p>
 * This has a few important consequences that need to be judged against against each other:
 * <ul>
 * <li>Persisting audit information is a bit slower, because an extra row is updated</li>
 * <li>Retrieving audit information is a lot faster</li>
 * </ul>
 * </p>
 *
 * @deprecated (since 5.4), use {@link org.hibernate.envers.strategy.internal.ValidityAuditStrategy} instead.
 *
 * @author Stephanie Pau
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
@Deprecated
public class ValidityAuditStrategy extends org.hibernate.envers.strategy.internal.ValidityAuditStrategy {

}
