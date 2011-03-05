/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashSet;

import org.hibernate.engine.jdbc.internal.TypeInfo;
import org.hibernate.engine.jdbc.internal.TypeInfoExtracter;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.test.common.ConnectionProviderBuilder;
import org.hibernate.testing.junit.UnitTestCase;
import org.hibernate.internal.util.collections.ArrayHelper;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class TypeInfoTest extends UnitTestCase {

	public TypeInfoTest(String string) {
		super( string );
	}

	public void testExtractTypeInfo() throws SQLException {
		ConnectionProvider connectionProvider = ConnectionProviderBuilder.buildConnectionProvider();
		Connection connection = connectionProvider.getConnection();
		LinkedHashSet<TypeInfo> typeInfoSet = TypeInfoExtracter.extractTypeInfo( connection.getMetaData() );
		for ( TypeInfo typeInfo : typeInfoSet ) {
			System.out.println( "~~~~~~ TYPE ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
			System.out.println( "             type name : " + typeInfo.getTypeName() );
			System.out.println( "             data type : " + typeInfo.getJdbcTypeCode() );
			System.out.println( "         create params : " + ArrayHelper.toString( typeInfo.getCreateParams() ) );
			System.out.println( "              unsigned : " + typeInfo.isUnsigned() );
			System.out.println( "             precision : " + typeInfo.getPrecision() );
			System.out.println( "         minimum scale : " + typeInfo.getMinimumScale() );
			System.out.println( "         maximum scale : " + typeInfo.getMaximumScale() );
			System.out.println( " fixed-precision scale : " + typeInfo.isFixedPrecisionScale() );
			System.out.println( "        literal prefix : " + typeInfo.getLiteralPrefix() );
			System.out.println( "        literal suffix : " + typeInfo.getLiteralSuffix() );
			System.out.println( "        case sensitive : " + typeInfo.isCaseSensitive() );
			System.out.println( "            searchable : " + typeInfo.getSearchability().toString() );
			System.out.println( "            nulability : " + typeInfo.getNullability().toString() );
			System.out.println( "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~" );
		}
	}
}
