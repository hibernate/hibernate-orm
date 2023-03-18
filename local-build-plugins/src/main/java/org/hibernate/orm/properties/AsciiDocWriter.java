/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.properties;


import java.io.IOException;
import java.io.Writer;
import java.util.Set;
import java.util.function.BiConsumer;

public class AsciiDocWriter implements BiConsumer<Set<ConfigurationProperty>, Writer> {

	private final String anchor;
	private final String title;

	public AsciiDocWriter(String anchor, String title) {
		this.anchor = anchor;
		this.title = title;
	}

	@Override
	public void accept(Set<ConfigurationProperty> properties, Writer writer) {
		try {
			tryToWriteLine( writer, "[[configuration-properties-aggregated-", anchor, "]]" );
			tryToWriteLine( writer, "=== ", title );
			writer.write( '\n' );
			for ( ConfigurationProperty el : properties ) {
				String key = el.key();
				writer.write( "[[" );
				writer.write( "configuration-properties-aggregated-" );
				writer.write( el.anchorPrefix() );
				writer.write( key.replaceAll( "[^\\w-.]", "-" ) );
				writer.write( "]] " );

				writer.write( '`' );
				writer.write( key );
				writer.write( '`' );
				writer.write( "::\n" );

				writer.write( el.javadoc() );

				writer.write( '\n' );
			}
			writer.write( '\n' );
		}
		catch (IOException e) {
			throw new RuntimeException( "Unable to create asciidoc output", e );
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
			throw new RuntimeException( "Unable to create asciidoc output", e );
		}
	}
}
