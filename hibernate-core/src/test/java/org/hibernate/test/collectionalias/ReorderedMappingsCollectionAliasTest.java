/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.collectionalias;

/**
 * The bug fixed by HHH-7545 showed showed different results depending on the order
 * in which entity mappings were processed.
 *
 * This mappings are in the opposite order here than in CollectionAliasTest.
 *
 * @Author Gail Badner
 */
public class ReorderedMappingsCollectionAliasTest extends CollectionAliasTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				ATable.class,
				TableA.class,
				TableB.class,
				TableBId.class,
		};
	}
}
