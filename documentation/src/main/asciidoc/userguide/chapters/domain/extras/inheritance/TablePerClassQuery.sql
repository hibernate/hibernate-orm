SELECT tablepercl0_.id AS id1_0_ ,
       tablepercl0_.balance AS balance2_0_ ,
       tablepercl0_.interestRate AS interest3_0_ ,
       tablepercl0_.owner AS owner4_0_ ,
       tablepercl0_.overdraftFee AS overdraf1_2_ ,
       tablepercl0_.creditLimit AS creditLi1_1_ ,
       tablepercl0_.clazz_ AS clazz_
FROM (
    SELECT    id ,
             balance ,
             interestRate ,
             owner ,
             CAST(NULL AS INT) AS overdraftFee ,
             CAST(NULL AS INT) AS creditLimit ,
             0 AS clazz_
    FROM     Account
    UNION ALL
    SELECT   id ,
             balance ,
             interestRate ,
             owner ,
             overdraftFee ,
             CAST(NULL AS INT) AS creditLimit ,
             1 AS clazz_
    FROM     DebitAccount
    UNION ALL
    SELECT   id ,
             balance ,
             interestRate ,
             owner ,
             CAST(NULL AS INT) AS overdraftFee ,
             creditLimit ,
             2 AS clazz_
    FROM     CreditAccount
) tablepercl0_