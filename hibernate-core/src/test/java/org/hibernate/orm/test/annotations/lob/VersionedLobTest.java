/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.lob;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Gail Badner
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsExpectedLobUsagePattern.class)
@DomainModel(
		annotatedClasses = {
				VersionedBook.class,
				VersionedCompiledCode.class
		}
)
public class VersionedLobTest extends AbstractLobTest<VersionedBook, VersionedCompiledCode> {
	@Override
	protected Class<VersionedBook> getBookClass() {
		return VersionedBook.class;
	}

	@Override
	protected Integer getId(VersionedBook book) {
		return book.getId();
	}

	@Override
	protected Class<VersionedCompiledCode> getCompiledCodeClass() {
		return VersionedCompiledCode.class;
	}

	@Override
	protected Integer getId(VersionedCompiledCode compiledCode) {
		return compiledCode.getId();
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testVersionUnchangedPrimitiveCharArray(SessionFactoryScope scope) {
		VersionedBook book = createBook();
		Editor editor = new Editor();
		editor.setName( "O'Reilly" );
		book.setEditor( editor );
		book.setCode2( new char[] {'r'} );

		scope.inTransaction(
				session ->
						session.persist( book )
		);

		scope.inTransaction(
				session -> {
					VersionedBook loadedBook = getBookClass().cast( session.find( getBookClass(), getId( book ) ) );
					assertThat( loadedBook.getVersion() ).isEqualTo( 0 );
					session.flush();
					assertThat( loadedBook.getVersion() ).isEqualTo( 0 );
					session.remove( loadedBook );
				}
		);

	}

	@Test
	public void testVersionUnchangedCharArray(SessionFactoryScope scope) {
		VersionedBook b = createBook();
		scope.inTransaction(
				session -> {
					b.setShortDescription( "Hibernate Bible" );
					b.setCode( new Character[] {'a', 'b', 'c'} );
					session.persist( b );
				}
		);

		scope.inTransaction(
				session -> {
					VersionedBook b2 = getBookClass().cast( session.find( getBookClass(), getId( b ) ) );
					assertThat( b2 ).isNotNull();
					assertThat( b2.getCode()[1].charValue() ).isEqualTo( b.getCode()[1].charValue() );
					assertThat( b2.getVersion() ).isEqualTo( 0 );
					session.flush();
					assertThat( b2.getVersion() ).isEqualTo( 0 );
					session.remove( b2 );
				}
		);
	}

	@Test
	public void testVersionUnchangedString(SessionFactoryScope scope) {
		VersionedBook b = createBook();
		scope.inTransaction(
				session -> {
					b.setShortDescription( "Hibernate Bible" );
					b.setFullText( "Hibernate in Action aims to..." );
					session.persist( b );
				}
		);

		scope.inTransaction(
				session -> {
					VersionedBook b2 = getBookClass().cast( session.find( getBookClass(), getId( b ) ) );
					assertThat( b2 ).isNotNull();
					assertThat( b2.getFullText() ).isEqualTo( b.getFullText() );
					assertThat( b2.getVersion() ).isEqualTo( 0 );
					session.flush();
					assertThat( b2.getVersion() ).isEqualTo( 0 );
					session.remove( b2 );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-5811")
	public void testVersionUnchangedByteArray(SessionFactoryScope scope) {
		VersionedCompiledCode cc = createCompiledCode();
		scope.inTransaction(
				session -> {
					Byte[] header = new Byte[2];
					header[0] = 3;
					header[1] = 0;
					cc.setHeader( header );
					session.persist( cc );
				}
		);

		scope.inTransaction(
				session -> {
					VersionedCompiledCode recompiled = getCompiledCodeClass().cast(
							session.find( getCompiledCodeClass(), getId( cc ) ) );
					assertThat( recompiled.getHeader()[1] ).isEqualTo( cc.getHeader()[1] );
					assertThat( recompiled.getVersion() ).isEqualTo( 0 );
					session.flush();
					assertThat( recompiled.getVersion() ).isEqualTo( 0 );
					session.remove( recompiled );
				}
		);
	}

	@Test
	public void testVersionUnchangedPrimitiveByteArray(SessionFactoryScope scope) {
		VersionedCompiledCode cc = createCompiledCode();
		int codeSize = 5;
		scope.inTransaction(
				session -> {
					byte[] full = new byte[codeSize];
					for ( int i = 0; i < codeSize; i++ ) {
						full[i] = (byte) (1 + i);
					}
					cc.setFullCode( full );
					session.persist( cc );
				}
		);

		scope.inTransaction(
				session -> {
					VersionedCompiledCode recompiled = getCompiledCodeClass().cast(
							session.find( getCompiledCodeClass(), getId( cc ) ) );
					assertThat( recompiled.getFullCode()[codeSize - 1] ).isEqualTo( cc.getFullCode()[codeSize - 1] );
					assertThat( recompiled.getVersion() ).isEqualTo( 0 );
					session.flush();
					assertThat( recompiled.getVersion() ).isEqualTo( 0 );
					session.remove( recompiled );
				}
		);
	}
}
