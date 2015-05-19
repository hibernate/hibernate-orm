/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.resource.transaction.backend.jta.internal.synchronization;

import java.io.Serializable;

/**
 * A pluggable strategy for defining any actions to be performed during
 * {@link javax.transaction.Synchronization#afterCompletion} processing from the the
 * {@link javax.transaction.Synchronization} registered by Hibernate with the underlying JTA platform.
 *
 * @author Steve Ebersole
 */
public interface AfterCompletionAction extends Serializable {
	void doAction(boolean successful);
}
