/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.test.locale;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Locale;

import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Brett Meyer
 */
@TestForIssue(jiraKey = "HHH-8579")
public class LocaleQueryAliasTest extends BaseCoreFunctionalTestCase {
	
	private static final String asciiRegex = "^\\p{ASCII}*$";
	
	private static Locale currentLocale;
	
	@Test
	public void testAliasWithLocale() {
		// Without the HHH-8579 fix, this will generate non-ascii query aliases.
		String hql = "from IAmAFoo";
		
		QueryTranslatorFactory ast = new ASTQueryTranslatorFactory();
		QueryTranslator queryTranslator = ast.createQueryTranslator(
				hql, hql, Collections.EMPTY_MAP, sessionFactory() );
		queryTranslator.compile( Collections.EMPTY_MAP, false );
		String sql = queryTranslator.getSQLString();
		
		assertTrue( sql.matches( asciiRegex ) );
	}
	
	@BeforeClass
	public static void beforeClass() {
		currentLocale = Locale.getDefault();
		
		// Turkish will generate a "dotless i" when toLowerCase is used on "I".
		Locale.setDefault(Locale.forLanguageTag("tr-TR"));
	}
	
	@AfterClass
	public static void afterClass() {
		Locale.setDefault(currentLocale);
	}
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { IAmAFoo.class };
	}
}
