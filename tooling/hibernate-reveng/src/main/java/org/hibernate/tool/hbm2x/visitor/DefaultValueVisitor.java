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
import org.hibernate.mapping.Value;
import org.hibernate.mapping.ValueVisitor;

/**
 * Default ValueVisitor which throws UnsupportedOperationException on all accepts.
 * Can be changed by passing true to the constructor.
 * 
 * @author max
 *
 */
public class DefaultValueVisitor implements ValueVisitor {

	boolean throwException = true;

	/**
	 * 
	 * @param throwException if true exception will be thrown, otherwise return null in accept calls.
	 */
	protected DefaultValueVisitor(boolean throwException) {
		this.throwException = throwException;
	}

	protected Object handle(Value o) {
		if (throwException) { 
			
			throw new UnsupportedOperationException("accept on " + o); 
		} 
		else { return null; }
	}
	
	public Object accept(Bag o){

		return handle(o);
	}

	public Object accept(IdentifierBag o){

		return handle(o);
	}

	public Object accept(List o){

		return handle(o);
	}

	public Object accept(PrimitiveArray o){

		return handle(o);
	}

	public Object accept(Array o){

		return handle(o);
	}

	public Object accept(Map o){

		return handle(o);
	}

	public Object accept(OneToMany o){

		return handle(o);
	}

	public Object accept(Set o){

		return handle(o);
	}

	public Object accept(Any o){

		return handle(o);
	}

	public Object accept(SimpleValue o){

		return handle(o);
	}

	public Object accept(DependantValue o){

		return handle(o);
	}

	public Object accept(Component o){

		return handle(o);
	}

	public Object accept(ManyToOne o){

		return handle(o);
	}

	public Object accept(OneToOne o){

		return handle(o);
	}

}
