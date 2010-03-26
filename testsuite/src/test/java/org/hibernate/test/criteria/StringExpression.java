package org.hibernate.test.criteria;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.LikeExpression;

public class StringExpression extends LikeExpression {

    protected StringExpression( String property, String value,
            boolean ignoreCase ) {
        super( property, value, null, ignoreCase );
    }

    public static Criterion stringExpression( String propertyName,
            String value, boolean ignoreCase ) {
        return new StringExpression( propertyName, value, ignoreCase );
    }
}
