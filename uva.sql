drop table uva cascade constraint;


create table uva (
tipouva varchar2(1),
nomeu varchar2(10),
acidezmin integer,
acidezmax integer
);

insert into uva values('a','albarinho',10,14);
insert into uva values('m','mencia',8,12);
insert into uva values('c','cainho',7,10);
insert into uva values('p','pedral',8,11);
insert into uva values('l','loureiro',6,10);
insert into uva values('g','garnacha',8,14);


commit;



