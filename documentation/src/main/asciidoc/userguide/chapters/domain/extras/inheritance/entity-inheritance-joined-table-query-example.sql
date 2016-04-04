SELECT jointablet0_.id AS id1_0_ ,
       jointablet0_.balance AS balance2_0_ ,
       jointablet0_.interestRate AS interest3_0_ ,
       jointablet0_.owner AS owner4_0_ ,
       jointablet0_1_.overdraftFee AS overdraf1_2_ ,
       jointablet0_2_.creditLimit AS creditLi1_1_ ,
       CASE WHEN jointablet0_1_.id IS NOT NULL THEN 1
            WHEN jointablet0_2_.id IS NOT NULL THEN 2
            WHEN jointablet0_.id IS NOT NULL THEN 0
       END AS clazz_
FROM   Account jointablet0_
       LEFT OUTER JOIN DebitAccount jointablet0_1_ ON jointablet0_.id = jointablet0_1_.id
       LEFT OUTER JOIN CreditAccount jointablet0_2_ ON jointablet0_.id = jointablet0_2_.id