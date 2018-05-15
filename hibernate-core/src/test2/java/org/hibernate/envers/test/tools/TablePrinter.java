/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.tools;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

/**
 * @author Chris Cranford
 */
public class TablePrinter {

	private static Map<Integer, Integer> buildColumnSizesFromRows(List<Tuple> rows) {
		Map<Integer, Integer> columnSizes = new TreeMap<>();
		for ( int i = 0; i < rows.size(); ++i ) {
			Tuple row = rows.get( i );
			// on first row, handle columns
			if ( i == 0 ) {
				for ( int j = 0; j < row.getElements().size(); ++j ) {
					TupleElement<?> element = row.getElements().get( j );
					columnSizes.put( j, element.getAlias().length() );
				}
			}
			// handle values
			for ( int j = 0; j < row.getElements().size(); ++j ) {
				Object value = row.get( j );
				int length = ( "" + value ).trim().length();
				if ( length > columnSizes.get( j ) ) {
					columnSizes.put( j, length );
				}
			}
		}
		return columnSizes;
	}

	private static void writeColumn(int columnIndex, int columns, int length, String value) {
		System.out.print( "| " + String.format( "%1$-" + length + "s", value ) + " " );
		if ( columnIndex + 1 >= columns ) {
			System.out.println( "|" );
		}
	}

	private static String buildSplitter(List<TupleElement<?>> columns, Map<Integer, Integer> columnSizes) {
		StringBuilder sb = new StringBuilder();
		for ( int i = 0; i < columns.size(); ++i ) {
			sb.append( "+-" + String.format( "%1$-" + columnSizes.get( i ) + "s", "" ).replace( ' ', '-' ) + '-' );
			if ( i + 1 >= columns.size() ) {
				sb.append( '+' );
			}
		}
		return sb.toString();
	}

	private static void printTableFromRows(List<Tuple> rows) {
		if ( rows.size() > 0 ) {
			Map<Integer, Integer> columnSizes = buildColumnSizesFromRows( rows );
			String rowSplitter = null;
			for ( int i = 0; i < rows.size(); ++i ) {
				Tuple row = rows.get( i );
				List<TupleElement<?>> columns = row.getElements();

				if ( rowSplitter == null ) {
					rowSplitter = buildSplitter( columns, columnSizes );
				}

				// handle columns on first row
				if ( i == 0 ) {
					System.out.println( rowSplitter );
					for ( int j = 0; j < columns.size(); ++j ) {
						writeColumn( j, columns.size(), columnSizes.get( j ), columns.get( j ).getAlias() );
					}
					System.out.println( rowSplitter );
				}

				// handle column values
				for ( int j = 0; j < columns.size(); ++j ) {
					final String value = row.get( j ) + "".trim();
					writeColumn( j, columns.size(), columnSizes.get( j ), value );
				}
			}

			System.out.println( rowSplitter );
		}
	}

	public static void print(EntityManager entityManager, String... tables) {
		for ( String table : tables ) {
			List<Tuple> rows = entityManager.createNativeQuery( "SELECT * FROM " + table + " ORDER BY REV", Tuple.class ).getResultList();
			printTableFromRows( rows );
		}
	}

	public static void print(Supplier<EntityManagerFactory> factorySupplier, String... tables) {
		doInJPA( factorySupplier, entityManager -> {
			print( entityManager, tables );
		} );
	}
}
