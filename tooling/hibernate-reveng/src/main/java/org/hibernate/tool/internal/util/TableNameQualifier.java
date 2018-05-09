package org.hibernate.tool.internal.util;

public class TableNameQualifier {

	public static String qualify(String catalog, String schema, String table) {
		StringBuilder qualifiedName = new StringBuilder();
		if ( catalog != null ) {
			qualifiedName.append( catalog ).append( '.' );
		}
		if ( schema != null ) {
			qualifiedName.append( schema ).append( '.' );
		}
		return qualifiedName.append( table ).toString();
	}
	
}
