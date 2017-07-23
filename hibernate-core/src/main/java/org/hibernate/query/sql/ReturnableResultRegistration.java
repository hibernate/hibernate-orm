/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql;

/**
 * Marker interface for ResultRegistration implementations which can be part
 * of the query result as a return "root"
 *
 * @author Steve Ebersole
 */
public interface ReturnableResultRegistration extends ResultRegistration {
}
