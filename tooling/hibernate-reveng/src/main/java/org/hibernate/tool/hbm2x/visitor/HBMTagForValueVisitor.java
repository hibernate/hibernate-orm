package org.hibernate.tool.hbm2x.visitor;

import org.hibernate.mapping.Any;
import org.hibernate.mapping.Array;
import org.hibernate.mapping.Bag;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.DependantValue;
import org.hibernate.mapping.IdentifierBag;
import org.hibernate.mapping.List;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.OneToMany;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PrimitiveArray;
import org.hibernate.mapping.Set;
import org.hibernate.mapping.SimpleValue;

public class HBMTagForValueVisitor extends DefaultValueVisitor {

	public static final HBMTagForValueVisitor INSTANCE = new HBMTagForValueVisitor();
	
	protected HBMTagForValueVisitor() {
		super(true);
	}
	
	public Object accept(Bag bag) {
		return "bag";
	}

	public Object accept(IdentifierBag bag) {
		return "idbag";
	}

	public Object accept(List list) {
		return "list";
	}

	public Object accept(Map map) {
		return "map";
	}

	public Object accept(OneToMany many) {
		return "one-to-many";
	}

	public Object accept(Set set) {
		return "set";
	}

	public Object accept(Any any) {
		return "any";
	}

	public Object accept(SimpleValue value) {		
		return "property";
	}

	public Object accept(PrimitiveArray primitiveArray) {
		return "primitive-array";
	}

	public Object accept(Array list) {
		return "array";
	}

	public Object accept(DependantValue value) {
		throw new IllegalArgumentException("No tag for " + value);
	}

	public Object accept(Component component) {
		return component.isDynamic()?"dynamic-component":"component";
	}

	public Object accept(ManyToOne mto) {
		return "many-to-one";
	}

	public Object accept(OneToOne oto) {
		return "one-to-one";
	}
}
