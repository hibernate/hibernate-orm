/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jdbc.util;

import java.util.StringTokenizer;

import org.junit.Test;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * BasicFormatterTest implementation
 *
 * @author Steve Ebersole
 */
public class BasicFormatterTest extends BaseUnitTestCase {

	@Test
	public void testNoLoss() {
		assertNoLoss( "insert into Address (city, state, zip, \"from\") values (?, ?, ?, 'insert value')" );
		assertNoLoss( "delete from Address where id = ? and version = ?" );
		assertNoLoss( "update Address set city = ?, state=?, zip=?, version = ? where id = ? and version = ?" );
		assertNoLoss( "update Address set city = ?, state=?, zip=?, version = ? where id in (select aid from Person)" );
		assertNoLoss(
				"select p.name, a.zipCode, count(*) from Person p left outer join Employee e on e.id = p.id and p.type = 'E' and (e.effective>? or e.effective<?) join Address a on a.pid = p.id where upper(p.name) like 'G%' and p.age > 100 and (p.sex = 'M' or p.sex = 'F') and coalesce( trim(a.street), a.city, (a.zip) ) is not null order by p.name asc, a.zipCode asc"
		);
		assertNoLoss(
				"select ( (m.age - p.age) * 12 ), trim(upper(p.name)) from Person p, Person m where p.mother = m.id and ( p.age = (select max(p0.age) from Person p0 where (p0.mother=m.id)) and p.name like ? )"
		);
		assertNoLoss(
				"select * from Address a join Person p on a.pid = p.id, Person m join Address b on b.pid = m.id where p.mother = m.id and p.name like ?"
		);
		assertNoLoss(
				"select case when p.age > 50 then 'old' when p.age > 18 then 'adult' else 'child' end from Person p where ( case when p.age > 50 then 'old' when p.age > 18 then 'adult' else 'child' end ) like ?"
		);
		assertNoLoss(
				"/* Here we' go! */ select case when p.age > 50 then 'old' when p.age > 18 then 'adult' else 'child' end from Person p where ( case when p.age > 50 then 'old' when p.age > 18 then 'adult' else 'child' end ) like ?"
		);
		assertNoLoss(
				"(select p.pid from Address where city = 'Boston') union (select p.pid from Address where city = 'Taipei')"
		);
		assertNoLoss( "select group0.[order] as order0 from [Group] group0 where group0.[order]=?1" );
	}

	private void assertNoLoss(String query) {
		String formattedQuery = FormatStyle.BASIC.getFormatter().format( query );
		StringTokenizer formatted = new StringTokenizer( formattedQuery, " \t\n\r\f()" );
		StringTokenizer plain = new StringTokenizer( query, " \t\n\r\f()" );

		log.debugf( "Original: {}", query );
		log.debugf( "Formatted: {}", formattedQuery );

		while ( formatted.hasMoreTokens() && plain.hasMoreTokens() ) {
			String plainToken = plain.nextToken();
			String formattedToken = formatted.nextToken();
			assertEquals( "formatter did not return the same token", plainToken, formattedToken );
		}
		assertFalse( formatted.hasMoreTokens() );
		assertFalse( plain.hasMoreTokens() );
	}
}
