create table memoquiz_quiz (
    id bigserial primary key,
    code varchar(50) not null unique,
    title varchar(200) not null,
    created_at timestamptz not null default now()
);

insert into memoquiz_quiz (code, title)
values ('default', 'Default Quiz');

create table memoquiz_quiz_card (
    quiz_id bigint not null,
    card_id bigint not null,
    enabled boolean not null default true,
    box smallint not null default 1,
    added_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    primary key (quiz_id, card_id),
    constraint memoquiz_quiz_card_box_check check (box between 1 and 7),
    constraint memoquiz_quiz_card_quiz_fk foreign key (quiz_id) references memoquiz_quiz (id) on delete cascade,
    constraint memoquiz_quiz_card_card_fk foreign key (card_id) references card (id) on delete cascade
);

create index memoquiz_quiz_card_lookup_idx
    on memoquiz_quiz_card (quiz_id, enabled, box);
