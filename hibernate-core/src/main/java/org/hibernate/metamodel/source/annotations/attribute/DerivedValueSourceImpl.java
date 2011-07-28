package org.hibernate.metamodel.source.annotations.attribute;

import org.hibernate.metamodel.source.binder.DerivedValueSource;

/**
 * @author Strong Liu
 */
public class DerivedValueSourceImpl implements DerivedValueSource {
    private final FormulaValue formulaValue;

    DerivedValueSourceImpl(FormulaValue formulaValue) {
        this.formulaValue = formulaValue;
    }

    @Override
    public String getExpression() {
        return formulaValue.getExpression();
    }

    @Override
    public String getContainingTableName() {
        return formulaValue.getContainingTableName();
    }
}
