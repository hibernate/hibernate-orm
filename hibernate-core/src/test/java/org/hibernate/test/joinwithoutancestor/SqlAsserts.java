/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.joinwithoutancestor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SqlAsserts {

	public static void assertFromTables(String sql, String... expectedTables) {
		List<Table> actualTables = parse( sql );
		if ( expectedTables.length == actualTables.size() ) {
			boolean diffFound = false;
			for ( int i = 0; i < expectedTables.length; i++ ) {
				if ( !( expectedTables[i].equals( actualTables.get( i ).name ) ) ) {
					diffFound = true;
					break;
				}
			}
			if ( !diffFound ) {
				return;
			}
		}
		List<String> actualTableNames = actualTables.stream().map( x -> x.name ).collect( Collectors.toList() );
		List<String> expectedTableNames = Arrays.asList( expectedTables );
		throw new AssertionError( "Expected tables: " + expectedTableNames + ", Actual tables: " + actualTableNames );
	}

	private static List<Table> parse(String sql) {
		List<Table> result = new ArrayList<>();
		String from = findFrom( sql );
		List<String> commaSeparatedFromParts = findCommaSeparatedFromParts( from );
		for ( String commaSeparatedFromPart : commaSeparatedFromParts ) {
			List<Table> tables = findTables( commaSeparatedFromPart );
			result.addAll( tables );
		}
		return result;
	}

	private static String findFrom(String sqlString) {
		Pattern pattern = Pattern.compile( ".*\\s+from\\s+(?<frompart>.*?)(\\z|(\\s+(where|order|having).*))" );
		Matcher matcher = pattern.matcher( sqlString );
		if ( matcher.matches() ) {
			return matcher.group( "frompart" );
		}
		else {
			throw new RuntimeException( "Can not find from part in sql statement." );
		}
	}

	private static List<String> findCommaSeparatedFromParts(String from) {
		return Arrays.stream( from.split( "," ) ).map( x -> x.trim() ).collect( Collectors.toList() );
	}

	private static List<Table> findTables(String fromPart) {
		List<Table> result = new ArrayList<>();
		result.add( findFirstTable( fromPart ) );

		String otherTablesPart = findOtherTablesPart( fromPart );
		result.addAll( findOtherTables( otherTablesPart ) );

		return result;
	}

	private static Table findFirstTable(String fromPart) {
		Pattern pattern = Pattern.compile( "(?<table>\\S+)\\s+(?<alias>\\S*)\\s*(?<joins>.*)" );
		Matcher matcher = pattern.matcher( fromPart );
		if ( matcher.matches() ) {
			Table firstTable = new Table( matcher.group( "table" ), matcher.group( "alias" ), false, false );
			return firstTable;
		}
		else {
			throw new RuntimeException( "Can not find the first table in the from part." );
		}
	}

	private static String findOtherTablesPart(String fromPart) {
		Pattern pattern = Pattern.compile( "(?<table>\\S+)\\s+(?<alias>\\S*)\\s*(?<joins>.*)" );
		Matcher matcher = pattern.matcher( fromPart );
		if ( matcher.matches() ) {
			String joins = matcher.group( "joins" );
			return joins;
		}
		else {
			throw new RuntimeException( "Can not find joins in the from part." );
		}
	}

	private static List<Table> findOtherTables(String otherTablesPart) {
		Pattern pattern = Pattern.compile(
				"(?<jointype>join|inner join|left join|cross join|left outer join)\\s+(?<table>\\S+)\\s+(?<alias>\\S+)" );
		Matcher matcher = pattern.matcher( otherTablesPart );
		List<Table> joins = new ArrayList<>();
		while ( matcher.find() ) {
			String table = matcher.group( "table" );
			String alias = matcher.group( "alias" );
			String join = matcher.group( "jointype" );
			boolean innerJoin = join.equals( "join" ) || join.equals( "inner join" );
			joins.add( new Table( table, alias, true, innerJoin ) );
		}
		return joins;
	}

	private static class Table {
		String name;
		String alias;
		boolean join;
		boolean innerJoin;

		public Table(String table, String alias, boolean join, boolean innerJoin) {
			this.name = table;
			this.alias = alias;
			this.join = join;
			this.innerJoin = innerJoin;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			if ( innerJoin ) {
				sb.append( "inner join " );
			}
			else if ( join ) {
				sb.append( "join " );
			}
			sb.append( name );
			sb.append( " " );
			sb.append( alias );
			return sb.toString();
		}
	}
}
