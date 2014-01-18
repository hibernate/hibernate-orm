package org.hibernate.hql.internal.ast.tree;

import javax.persistence.AttributeConverter;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;

/**
 * @author Oleksander Dukhno
 */
public class EnumNode extends JavaConstantNode {

	private AttributeConverterTypeAdapter converterTypeAdapter;

	public void setText(String text) {
		super.setText( text );
		if ( !StringHelper.isEmpty( text ) ) {
			Object enumConstant = ReflectHelper.getConstantValue( text );
			if ( enumConstant.getClass().isEnum() ) {
				setConstantValue( enumConstant );
			}
		}
	}

	public void setExpectedType(Type expectedType) {
		if ( getExpectedType() != null ) {
			return;
		}
		super.setExpectedType( expectedType );
		if ( AttributeConverterTypeAdapter.class.isInstance( expectedType ) ) {
			AttributeConverterTypeAdapter adapter = (AttributeConverterTypeAdapter) expectedType;
			if ( adapter.getModelType().isEnum() ) {
				super.setExpectedType( expectedType );
				converterTypeAdapter = adapter;
			}
		}
	}

	public String getRenderText(SessionFactoryImplementor sessionFactory) {
		if ( converterTypeAdapter != null ) {
			AttributeConverter converter = converterTypeAdapter.getAttributeConverter();
			StringBuilder result = new StringBuilder(
					converter.convertToDatabaseColumn( getConstantValue() )
							.toString()
			);
			result.insert( 0, '\'' );
			result.append( '\'' );
			return result.toString();
		}
		return super.getRenderText( sessionFactory );
	}

}
