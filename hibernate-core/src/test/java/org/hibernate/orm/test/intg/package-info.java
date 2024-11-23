/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Tests verifying that expectations of various integrations continue to work.
 *
 * E.g. make sure that SQM trees can
 * continue to be built without access to SessionFactory; Quarkus and others use this to translate HQL statements
 * into SQM at boot time or even build time
 */
package org.hibernate.orm.test.intg;
