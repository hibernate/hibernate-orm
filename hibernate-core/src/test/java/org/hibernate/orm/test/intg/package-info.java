/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Tests verifying that expectations of various integrations continue to work.
 *
 * E.g. make sure that SQM trees can
 * continue to be built without access to SessionFactory; Quarkus and others use this to translate HQL statements
 * into SQM at boot time or even build time
 */
package org.hibernate.orm.test.intg;
