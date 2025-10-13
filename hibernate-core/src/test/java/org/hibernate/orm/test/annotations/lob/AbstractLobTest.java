/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.lob;


import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gail Badner
 */
@SessionFactory
public abstract class AbstractLobTest<B extends AbstractBook, C extends AbstractCompiledCode> {

	protected abstract Class<B> getBookClass();

	protected B createBook() {
		try {
			return getBookClass().newInstance();
		}
		catch (Exception ex) {
			throw new RuntimeException( "Could not create an instance of type " + getBookClass().getName(), ex );
		}
	}

	protected abstract Integer getId(B book);

	protected abstract Class<C> getCompiledCodeClass();

	protected C createCompiledCode() {
		try {
			return getCompiledCodeClass().newInstance();
		}
		catch (Exception ex) {
			throw new RuntimeException( "Could not create an instance of type " + getCompiledCodeClass().getName(),
					ex );
		}
	}

	protected abstract Integer getId(C compiledCode);

	@Test
	public void testSerializableToBlob(SessionFactoryScope scope) {
		B book = createBook();
		Editor editor = new Editor();
		editor.setName( "O'Reilly" );
		book.setEditor( editor );
		book.setCode2( new char[] {'r'} );

		scope.inTransaction( session ->
				session.persist( book )
		);

		scope.inTransaction( session -> {
			B loadedBook = getBookClass().cast( session.get( getBookClass(), getId( book ) ) );
			assertThat( loadedBook.getEditor() ).isNotNull();
			assertThat( loadedBook.getEditor().getName() ).isEqualTo( book.getEditor().getName() );
			loadedBook.setEditor( null );
		} );

		scope.inTransaction( session -> {
			B loadedBook = getBookClass().cast( session.get( getBookClass(), getId( book ) ) );
			assertThat( loadedBook.getEditor() ).isNull();
		} );
	}

	@Test
	public void testClob(SessionFactoryScope scope) {

		B book = createBook();
		book.setShortDescription( "Hibernate Bible" );
		book.setFullText( "Hibernate in Action aims to..." );
		book.setCode( new Character[] {'a', 'b', 'c'} );
		book.setCode2( new char[] {'a', 'b', 'c'} );

		scope.inTransaction( session ->
				session.persist( book )
		);

		scope.inTransaction( session -> {
			B b2 = getBookClass().cast( session.get( getBookClass(), getId( book ) ) );
			assertThat( b2 ).isNotNull();
			assertThat( b2.getFullText() ).isEqualTo( book.getFullText() );
			assertThat( b2.getCode()[1].charValue() ).isEqualTo( book.getCode()[1].charValue() );
			assertThat( b2.getCode2()[2] ).isEqualTo( book.getCode2()[2] );
		} );
	}

	@Test
	public void testBlob(SessionFactoryScope scope) {

		C cc = createCompiledCode();
		Byte[] header = new Byte[2];
		header[0] = 3;
		header[1] = 0;
		cc.setHeader( header );
		int codeSize = 5;
		byte[] full = new byte[codeSize];
		for ( int i = 0; i < codeSize; i++ ) {
			full[i] = (byte) (1 + i);
		}
		cc.setFullCode( full );

		scope.inTransaction( session ->
				session.persist( cc )
		);

		scope.inTransaction( session -> {
			C recompiled = getCompiledCodeClass().cast( session.get( getCompiledCodeClass(), getId( cc ) ) );
			assertThat( recompiled.getHeader()[1] ).isEqualTo( cc.getHeader()[1] );
			assertThat( recompiled.getFullCode()[codeSize - 1] ).isEqualTo( cc.getFullCode()[codeSize - 1] );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = SybaseDialect.class)
	public void testBinary(SessionFactoryScope scope) {

		C cc = createCompiledCode();
		byte[] metadata = new byte[2];
		metadata[0] = 3;
		metadata[1] = 0;
		cc.setMetadata( metadata );

		scope.inTransaction( session ->
				session.persist( cc )
		);

		scope.inTransaction( session -> {
			C recompiled = getCompiledCodeClass().cast( session.get( getCompiledCodeClass(), getId( cc ) ) );
			assertThat( recompiled.getMetadata()[1] ).isEqualTo( cc.getMetadata()[1] );
		} );
	}
}
