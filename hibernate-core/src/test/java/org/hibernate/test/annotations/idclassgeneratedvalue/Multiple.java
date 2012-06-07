/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.idclassgeneratedvalue;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.GenericGenerator;

/**
 * An Entity containing a composite key with two generated values.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Stale W. Pedersen</a>
 */
@Entity
@IdClass(MultiplePK.class)
@SuppressWarnings("serial")
public class Multiple implements Serializable
{
   @Id
   @GenericGenerator(name = "increment", strategy = "increment")
   @GeneratedValue(generator = "increment")
   private Long id1;
   
   @Id
   @GeneratedValue(generator = "MULTIPLE_SEQ", strategy = GenerationType.SEQUENCE)
   @SequenceGenerator( name = "MULTIPLE_SEQ", sequenceName = "MULTIPLE_SEQ")
   private Long id2;
   
   @Id
   private Long id3;
   private int quantity;
   
   public Multiple()
   {
      
   }
   
   public Multiple(Long id3, int quantity)
   {
      this.id3 = id3;
      this.quantity = quantity;
   }

   public Long getId1()
   {
      return id1;
   }

   public Long getId2()
   {
      return id2;
   }

   public Long getId3()
   {
      return id3;
   }

   public int getQuantity()
   {
      return quantity;
   }

   public void setQuantity(int quantity)
   {
      this.quantity = quantity;
   }
   
   
}
