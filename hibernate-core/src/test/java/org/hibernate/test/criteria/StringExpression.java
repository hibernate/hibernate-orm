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
