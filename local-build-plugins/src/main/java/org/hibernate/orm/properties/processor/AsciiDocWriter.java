/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.properties.processor;


import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AsciiDocWriter implements BiConsumer<Map<String, ConfigurationProperty>, Writer> {

	private final Predicate<Map.Entry<String, ConfigurationProperty>> filter;

	public AsciiDocWriter(Predicate<Map.Entry<String, ConfigurationProperty>> filter) {
		this.filter = filter;
	}

	@Override
	public void accept(Map<String, ConfigurationProperty> propertyMap, Writer writer) {
		Map<String, Collection<ConfigurationProperty>> groups = propertyMap.entrySet().stream()
				.filter( filter )
				.map( Map.Entry::getValue )
				.collect(
						Collectors.groupingBy(
								ConfigurationProperty::moduleName,
								TreeMap::new,
								Collectors.toCollection( TreeSet::new )
						)
				);

		if ( groups.isEmpty() ) {
			// nothing to write - return fast.
			return;
		}

		try {
			for ( Map.Entry<String, Collection<ConfigurationProperty>> entry : groups.entrySet() ) {
				tryToWriteLine( writer, "[[configuration-properties-aggregated-", entry.getValue().iterator().next().anchorPrefix(), "]]" );
				tryToWriteLine( writer, "=== ", entry.getKey() );
				writer.write( '\n' );
				for ( ConfigurationProperty el : entry.getValue() ) {
					Iterator<String> keys = el.key().resolvedKeys().iterator();
					String firstKey = keys.next();
					writer.write( "[[" );
					writer.write( "configuration-properties-aggregated-" );
					writer.write( el.anchorPrefix() );
					writer.write( firstKey.replaceAll( "[^\\w-.]", "_" ) );
					writer.write( "]] " );

					writer.write( '`' );
					writer.write( firstKey );
					writer.write( '`' );
					writer.write( "::\n" );

					// using inline passthrough for javadocs to not render HTML.
					writer.write( "+++ " );
					writer.write( el.javadoc() );
					writer.write( " +++ " );

					writer.write( '\n' );

					printOtherKeyVariants( writer, keys );
				}
			}
			writer.write( '\n' );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	private void printOtherKeyVariants(Writer writer, Iterator<String> keys) throws IOException {
		boolean hasMultipleKeys = false;
		if ( keys.hasNext() ) {
			hasMultipleKeys = true;
			writer.write( "+\n" );
			writer.write( ".Variants of this configuration property (Click here):\n" );
			writer.write( "[%collapsible]\n" );
			writer.write( "====\n" );
		}
		while ( keys.hasNext() ) {
			writer.write( '`' );
			writer.write( keys.next() );
			writer.write( '`' );
			if ( keys.hasNext() ) {
				writer.write( ", " );
			}
		}

		if ( hasMultipleKeys ) {
			writer.write( "\n====\n" );
		}
	}

	private void tryToWriteLine(Writer writer, String prefix, String value, String... other) {
		try {
			writer.write( prefix );
			writer.write( value );
			for ( String s : other ) {
				writer.write( s );
			}
			writer.write( "\n" );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}
}
