/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

@NamedQuery(name = "allCarpet", query = "select c from Carpet c")
package org.hibernate.jpa.test.pack.explodedpar;

import org.hibernate.annotations.NamedQuery;
