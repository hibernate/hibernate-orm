/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.spi.path;

import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
public class NavigablePathTests {
	@Test
	public void testRoots() {
		final NavigablePath root = new NavigablePath( "org.hibernate.Root", "r" );
		assertThat( root.equals( root ) ).isTrue();
		assertThat( root.getFullPath() ).isEqualTo( "org.hibernate.Root(r)" );
		assertThat( root.getLocalName() ).isEqualTo( "org.hibernate.Root" );
		assertThat( root.isAliased() ).isTrue();
		assertThat( root.getAlias() ).isEqualTo( "r" );

		final NavigablePath root2 = new NavigablePath( "org.hibernate.Root", "r" );
		assertThat( root.equals( root2 ) ).isTrue();
	}

	@Test
	public void testSubPaths() {
		final NavigablePath root = new NavigablePath( "org.hibernate.Root", "r" );

		final NavigablePath name1 = root.append( "name" );
		final NavigablePath name2 = root.append( "name" );

		assertThat( name1.equals( name2 ) ).isTrue();

		final NavigablePath id = root.append( "id" );

		assertThat( name1.equals( root ) ).isFalse();
		assertThat( id.equals( root ) ).isFalse();
	}

	@Test
	public void testParallelSubPaths() {
		final NavigablePath root1 = new NavigablePath( "org.hibernate.Root", "r" );
		final NavigablePath root2 = new NavigablePath( "org.hibernate.Root", "r" );

		final NavigablePath name1 = root1.append( "name" );
		final NavigablePath name2 = root2.append( "name" );

		assertThat( name1.equals( name2 ) ).isTrue();
	}

	@Test
	public void testNestedPaths() {
		final NavigablePath root = new NavigablePath( "org.hibernate.Root", "r" );

		final NavigablePath comp1 = root.append( "comp" );
		final NavigablePath comp2 = root.append( "comp" );

		final NavigablePath comp1Name = comp1.append( "name" );
		final NavigablePath comp2Name = comp2.append( "name" );

		assertThat( comp1Name.equals( comp2Name ) ).isTrue();
	}

	@Test
	public void testDivergentNestedPaths() {
		final NavigablePath root = new NavigablePath( "org.hibernate.Root", "r" );

		final NavigablePath comp = root.append( "comp" );
		final NavigablePath other = root.append( "other" );

		final NavigablePath compName = comp.append( "name" );
		final NavigablePath otherName = other.append( "name" );

		assertThat( compName.equals( otherName ) ).isFalse();
	}

	@Test
	public void testStringification() {
		final String rootStr = "org.hibernate.Root";
		final String aliasedRootStr = "org.hibernate.Root(r)";
		final String nameStr = "name";
		final String namePathStr = aliasedRootStr + "." + nameStr;

		final NavigablePath root = new NavigablePath( rootStr, "r" );

		final NavigablePath name = root.append( "name" );
		assertThat( name.getLocalName() ).isEqualTo( nameStr );
		assertThat( name.getFullPath() ).isEqualTo( namePathStr );
	}

	@Test
	public void testIdentifierPaths() {
		final String rootStr = "org.hibernate.Root";
		final String aliasedRootStr = "org.hibernate.Root(r)";

		final String pkStr = "pk";
		final String pkFullPathStr = aliasedRootStr + "." + EntityIdentifierMapping.ID_ROLE_NAME;

		final NavigablePath root = new NavigablePath( rootStr, "r" );

		final NavigablePath idPath = new EntityIdentifierNavigablePath( root, pkStr );
		assertThat( idPath.getLocalName() ).isEqualTo( EntityIdentifierMapping.ID_ROLE_NAME );
		assertThat( idPath.getFullPath() ).isEqualTo( pkFullPathStr );
	}
}
