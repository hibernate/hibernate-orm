/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria;

import org.hibernate.query.sqm.FrameExclusion;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;

/**
 * @author Marco Belladelli
 */
public interface JpaWindow {
	JpaWindow partitionBy(Expression<?>... expressions);

	JpaWindow orderBy(Order... expressions);

	JpaWindow frameRows(JpaWindowFrame startFrame, JpaWindowFrame endFrame);

	JpaWindow frameRange(JpaWindowFrame startFrame, JpaWindowFrame endFrame);

	JpaWindow frameGroups(JpaWindowFrame startFrame, JpaWindowFrame endFrame);

	JpaWindow frameExclude(FrameExclusion frameExclusion);
}
