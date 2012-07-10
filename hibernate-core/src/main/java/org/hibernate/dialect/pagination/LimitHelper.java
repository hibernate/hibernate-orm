package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class LimitHelper {
	public static boolean useLimit(LimitHandler limitHandler, RowSelection selection) {
		return limitHandler.supportsLimit() && hasMaxRows( selection );
	}

	public static boolean hasFirstRow(RowSelection selection) {
		return getFirstRow( selection ) > 0;
	}

	public static int getFirstRow(RowSelection selection) {
		return ( selection == null || selection.getFirstRow() == null ) ? 0 : selection.getFirstRow();
	}

	public static boolean hasMaxRows(RowSelection selection) {
		return selection != null && selection.getMaxRows() != null && selection.getMaxRows() > 0;
	}
}
