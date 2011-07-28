package org.hibernate.metamodel.source.annotations.attribute;

/**
 * @author Strong Liu
 */
public class FormulaValue {
    private String tableName;
    private final String expression;

    public FormulaValue(String tableName, String expression) {
        this.tableName = tableName;
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    public String getContainingTableName() {
        return tableName;
    }
}
