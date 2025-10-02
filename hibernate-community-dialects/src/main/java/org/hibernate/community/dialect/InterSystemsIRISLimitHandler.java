/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect.pagination;

import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.query.spi.Limit;

import java.util.Locale;

/**
 * @author Wei Xia
 */
public class InterSystemsIRISLimitHandler extends AbstractLimitHandler {
    public static final InterSystemsIRISLimitHandler INSTANCE = new InterSystemsIRISLimitHandler(true);

    private final boolean variableLimit;

    public InterSystemsIRISLimitHandler(boolean variableLimit) {
        this.variableLimit = variableLimit;
    }

    @Override
    public String processSql(String sql, Limit limit) {

        boolean hasFirstRow = hasFirstRow(limit);
        boolean hasMaxRows = hasMaxRows(limit);

        if (!hasFirstRow && !hasMaxRows) {
            return sql;
        }


        String lowersql = sql.toLowerCase(Locale.ROOT);
        int selectIndex = lowersql.indexOf("select");
        if (hasFirstRow) {
            return new StringBuilder(sql.length() + 27)
                    .append(sql)
                    .insert(selectIndex + 6, " %ROWOFFSET ? %ROWLIMIT ? ")
                    .toString();
        } else {
            final int selectDistinctIndex = lowersql.indexOf("select distinct");
            final int insertionPoint = selectIndex + (selectDistinctIndex == selectIndex ? 15 : 6);

            return new StringBuilder(sql.length() + 8)
                    .append(sql)
                    .insert(insertionPoint, " TOP ? ")
                    .toString();
        }


    }


    @Override
    public final boolean supportsLimit() {
        return true;
    }

    @Override
    public final boolean supportsOffset() {
        return true;
    }

    @Override
    public boolean supportsLimitOffset() {
        return true;
    }

    @Override
    public final boolean supportsVariableLimit() {
        return true;
    }

    @Override
    public boolean useMaxForLimit() {
        return true;
    }

}
