create table inbox (
    event_id uuid primary key,
    processed_at timestamptz not null default now()
);

create table outbox (
    id uuid primary key,
    file_id varchar(255),
    payload jsonb not null,
    status varchar(10) not null default 'PENDING'
        constraint chk_status check ( status in ('PENDING', 'SUCCESS', 'FAILED') ),
    created_at timestamptz not null default now(),
    published_at timestamptz
);

create index idx_outbox_pending_status on outbox(created_at) where status = 'PENDING';