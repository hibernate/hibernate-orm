select
    cr.person_id as person_i1_0_0_,
    cr.call_register as call_reg2_0_0_,
    cr.country_code as country_3_0_,
    cr.operator_code as operator4_0_,
    cr.subscriber_code as subscrib5_0_
from
    call_register cr
where
    cr.person_id = ?

-- binding parameter [1] as [BIGINT] - [1]

-- extracted value ([person_i1_0_0_] : [BIGINT])  - [1]
-- extracted value ([call_reg2_0_0_] : [INTEGER]) - [101]
-- extracted value ([country_3_0_]   : [VARCHAR]) - [01]
-- extracted value ([operator4_0_]   : [VARCHAR]) - [234]
-- extracted value ([subscrib5_0_]   : [VARCHAR]) - [567]

-- extracted value ([person_i1_0_0_] : [BIGINT])  - [1]
-- extracted value ([call_reg2_0_0_] : [INTEGER]) - [102]
-- extracted value ([country_3_0_]   : [VARCHAR]) - [01]
-- extracted value ([operator4_0_]   : [VARCHAR]) - [234]
-- extracted value ([subscrib5_0_]   : [VARCHAR]) - [789]