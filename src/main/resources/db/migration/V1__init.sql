create table inbox (
    event_id uuid primary key,
    status varchar(10) not null default 'PENDING'
        constraint chk_inbox_status check ( status in ('PENDING', 'SUCCESS', 'FAILED', 'PROCESSING') ),
    locked_until timestamptz,
    locked_token uuid,
    retry_attempts int not null default 0,
    processed_at timestamptz,
    created_at timestamptz not null default now()
);

create table outbox (
    id uuid primary key,
    file_id varchar(255),
    correlation_id uuid not null,
    payload jsonb not null,
    status varchar(10) not null default 'PENDING'
        constraint chk_outbox_status check ( status in ('PENDING', 'SUCCESS', 'FAILED', 'PROCESSING') ),
    locked_until timestamptz,
    locked_token uuid,
    retry_attempts int not null default 0,
    next_attempt_at timestamptz default now(),
    last_error text,
    created_at timestamptz not null default now(),
    published_at timestamptz
);

create index idx_inbox_status_locked on inbox (status, locked_until);
create index idx_outbox_ready on outbox (status, next_attempt_at);

create unique index uq_outbox_correlation_id on outbox (correlation_id);
