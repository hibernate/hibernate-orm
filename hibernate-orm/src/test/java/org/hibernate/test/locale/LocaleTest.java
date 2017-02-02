/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.locale;

import java.util.Collections;
import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.tool.hbm2ddl.SchemaValidator;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Brett Meyer
 */
public class LocaleTest extends BaseNonConfigCoreFunctionalTestCase {
	
	private static final String asciiRegex = "^\\p{ASCII}*$";
	
	private static Locale currentLocale;
	
	@Test
	@TestForIssue(jiraKey = "HHH-8579")
	public void testAliasWithLocale() {
		// Without the HHH-8579 fix, this will generate non-ascii query aliases.
		String hql = "from IAmAFoo";
		
		QueryTranslatorFactory ast = new ASTQueryTranslatorFactory();
		QueryTranslator queryTranslator = ast.createQueryTranslator(
				hql, hql, Collections.EMPTY_MAP, sessionFactory(), null );
		queryTranslator.compile( Collections.EMPTY_MAP, false );
		String sql = queryTranslator.getSQLString();
		
		assertTrue( sql.matches( asciiRegex ) );
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-8765")
	public void testMetadataWithLocale() {
		try {
			// Rather than building TableMetadata and checking for ascii values in table/column names, simply
			// attempt to validate.
			new SchemaValidator().validate( metadata() );
		}
		catch (HibernateException e) {
			fail("Failed with the Turkish locale, most likely due to the use of String#toLowerCase() within hbm2ddl.  "
					+ "Search for all instaces and replace with StringHelper#toLowerCase(String)!  " + e.getMessage());
		}
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
