/*
 * Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, v. 2.1. This program is distributed in the
 * hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License, v.2.1 along with this
 * distribution; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Red Hat Author(s): Steve Ebersole
 */
package org.hibernate.jdbc.util;

import java.util.StringTokenizer;

import junit.framework.TestCase;

/**
 * BasicFormatterTest implementation
 *
 * @author Steve Ebersole
 */
public class BasicFormatterTest extends TestCase {
	public BasicFormatterTest(String name) {
		super( name );
	}

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
	}

	private void assertNoLoss(String query) {
		String formattedQuery = FormatStyle.BASIC.getFormatter().format( query );
		StringTokenizer formatted = new StringTokenizer( formattedQuery, " \t\n\r\f()" );
		StringTokenizer plain = new StringTokenizer( query, " \t\n\r\f()" );

		System.out.println( "Original: " + query );
		System.out.println( "Formatted: " + formattedQuery );
		while ( formatted.hasMoreTokens() && plain.hasMoreTokens() ) {
			String plainToken = plain.nextToken();
			String formattedToken = formatted.nextToken();
			assertEquals( "formatter did not return the same token", plainToken, formattedToken );
		}
		assertFalse( formatted.hasMoreTokens() );
		assertFalse( plain.hasMoreTokens() );
	}
}
