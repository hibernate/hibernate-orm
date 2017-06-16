/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

/**
 * Actions to perform in regards to an id-table prior to each use.
 *
 * @author Steve Ebersole
 */
public enum BeforeUseAction {
	CREATE,
	NONE
}
