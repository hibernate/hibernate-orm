/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.common;

/**
 * @author Christian Beikov
 */
public enum FrameKind {
	UNBOUNDED_PRECEDING,
	OFFSET_PRECEDING,
	CURRENT_ROW,
	OFFSET_FOLLOWING,
	UNBOUNDED_FOLLOWING
}
