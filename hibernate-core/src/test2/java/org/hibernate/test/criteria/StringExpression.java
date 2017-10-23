/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.LikeExpression;

public class StringExpression extends LikeExpression {
    private final static Character ESCAPE_CODE = new Character( '\\' );

    protected StringExpression( String property, String value,
            boolean ignoreCase ) {
        super( property, value, ESCAPE_CODE, ignoreCase );
    }

    public static Criterion stringExpression( String propertyName,
            String value, boolean ignoreCase ) {
        return new StringExpression( propertyName, value, ignoreCase );
    }
}
