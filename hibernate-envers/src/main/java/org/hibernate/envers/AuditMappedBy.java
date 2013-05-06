package org.hibernate.envers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Annotation to specify a "fake" bi-directional relation. Such a relation uses {@code @OneToMany} +
 * {@code @JoinColumn} on the one side, and {@code @ManyToOne} + {@code @Column(insertable=false, updatable=false)} on
 * the many side. Then, Envers won't use a join table to audit this relation, but will store changes as in a normal
 * bi-directional relation.
 * </p>
 * <p/>
 * <p>
 * This annotation is <b>experimental</b> and may change in future releases.
 * </p>
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface AuditMappedBy {
	/**
	 * Name of the property in the related entity which maps back to this entity. The property should be
	 * mapped with {@code @ManyToOne} and {@code @Column(insertable=false, updatable=false)}.
	 */
	String mappedBy();

	/**
	 * Name of the property in the related entity which maps to the position column. Should be specified only
	 * for indexed collection, when @{@link org.hibernate.annotations.IndexColumn} or
	 * {@link javax.persistence.OrderColumn} is used on the collection.  The property should be mapped with
	 * {@code @Column(insertable=false, updatable=false)}.
	 */
	String positionMappedBy() default "";
}
