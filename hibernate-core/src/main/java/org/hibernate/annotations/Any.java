/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.annotations;
import java.lang.annotation.Retention;
import javax.persistence.Column;
import javax.persistence.FetchType;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines a ToOne-style association pointing to one of several entity types depending on a local discriminator,
 * as opposed to discriminated inheritance where the discriminator is kept as part of the entity hierarchy.
 *
 * For example, if you consider an Order entity containing Payment information where Payment might be of type
 * CashPayment or CreditCardPayment the @Any approach would be to keep that discriminator and matching value on the
 * Order itself.  Thought of another way, the "foreign-key" really is made up of the value and discriminator
 * (there is no physical foreign key here as databases do not support this):
 * <blockquote><pre>
 *    &#064;Entity
 *    class Order {
 *        ...
 *        &#064;Any( metaColumn = @Column( name="payment_type" ) )
 *        &#064;AnyMetDef(
 *                idType = "long"
 *                metaValues = {
 *                        &#064;MetaValue( value="C", targetEntity=CashPayment.class ),
 *                        &#064;MetaValue( value="CC", targetEntity=CreditCardPayment.class ),
 *                }
 *        )
 *        pubic Payment getPayment() { ... }
 *    }
 * }
 * </pre></blockquote>
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 *
 * @see AnyMetaDef
 */
@java.lang.annotation.Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Any {
	/**
	 * Metadata definition used.
	 * If defined, should point to a @AnyMetaDef name
	 * If not defined, the local (ie in the same field or property) @AnyMetaDef is used
	 */
	String metaDef() default "";

	/**
	 * Identifies the discriminator column.  This column will hold the value that identifies the targeted entity.
	 */
	Column metaColumn();

	/**
	 * Defines whether the value of the field or property should be lazily loaded or must be
	 * eagerly fetched. The EAGER strategy is a requirement on the persistence provider runtime
	 * that the value must be eagerly fetched. The LAZY strategy is applied when bytecode
	 * enhancement is used. If not specified, defaults to EAGER.
	 */
	FetchType fetch() default FetchType.EAGER;
	/**
	 * Whether the association is optional. If set to false then a non-null relationship must always exist.
	 */
	boolean optional() default true;
}
